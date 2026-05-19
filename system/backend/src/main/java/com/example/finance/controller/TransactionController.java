package com.example.finance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.finance.common.Result;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.entity.dto.TransactionRequest;
import com.example.finance.entity.dto.TransferDTO;
import com.example.finance.entity.dto.TransferRequest;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 交易记录控制器（PRD P0-4 收支记录 + P1-1 多条件筛选 + P1-5 转账 + P2-3 CSV 批量导入）
 *
 * 职责：接收收支记录的 HTTP 请求，参数校验后转发 TransactionService 处理
 * 路由前缀：/api/transaction
 * 依赖：→ TransactionService（业务逻辑层）→ TransactionMapper + AccountMapper（数据访问层）
 *
 * 接口清单：
 *   GET    /api/transaction              — 查询交易记录（分页 + 多条件筛选）
 *   POST   /api/transaction              — 创建收支记录（记一笔）
 *   PUT    /api/transaction/{id}         — 更新收支记录
 *   POST   /api/transaction/transfer     — 转账（生成两条关联记录）
 *   POST   /api/transaction/import       — CSV 批量导入（≤5MB，≤1000 条）
 *
 * 被前端调用：→ api/transaction.js 的 getTransactionList/create/update/transfer/importCsv
 * 被 TransactionListPage.vue、TransferPage.vue、ImportPage.vue 调用
 */
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

  /** → TransactionService：处理收支记录 CRUD + 转账 + CSV 导入的业务逻辑 */
  private final TransactionService transactionService;

  /**
   * 查询交易记录接口（分页 + 多条件筛选 · PRD P0-4 + P1-1）
   *
   * 流程：从 JWT 提取 userId → 构建动态筛选条件（账户/分类/时间范围/关键词）
   *     → TransactionMapper XML 动态 SQL 查询（JOIN account + category 获取名称）
   *     → 分页返回结果
   *
   * @param accountId  账户 ID 筛选（可选）
   * @param categoryId 分类 ID 筛选（可选）
   * @param startTime  开始时间筛选（可选，格式 yyyy-MM-dd HH:mm:ss）
   * @param endTime    结束时间筛选（可选）
   * @param keyword    关键词搜索（模糊匹配备注，可选）
   * @param sortBy     排序方式：time/amount_asc/amount_desc（默认 time 降序）
   * @param pageNum    页码（默认 1）
   * @param pageSize   每页条数（默认 10）
   * @param request    HTTP 请求
   * @return Result<IPage<TransactionDTO>> 分页交易记录（含账户名、分类名）
   *
   * 被前端 TransactionListPage.vue 列表展示 + 筛选表单调用
   * 查询逻辑在 TransactionMapper.xml 的 selectTransactionList/selectTransactionCount
   */
  @GetMapping
  public Result<IPage<TransactionDTO>> list(
      @RequestParam(required = false) Long accountId,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false, defaultValue = "time") String sortBy,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize,
      HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → TransactionService.list()：动态 SQL 筛选 + 分页 + JOIN 关联查询
    IPage<TransactionDTO> page = transactionService.list(
        userId, accountId, categoryId, startTime, endTime, keyword, sortBy, pageNum, pageSize);
    return Result.success(page);
  }

  /**
   * 创建交易记录接口（记一笔 · PRD P0-4）
   *
   * 流程：@Valid 校验金额/类型/账户/分类 → 校验账户状态（必须正常）
   *     → 插入 transaction 表 → 返回新记录
   *
   * @param request 交易记录请求体（accountId/categoryId/type/amount/note/time，含 @Valid 校验）
   * @param httpRequest HTTP 请求
   * @return Result<TransactionDTO> 新创建的交易记录
   *
   * 被前端 TransactionListPage.vue「记一笔」弹窗调用
   * type：1=收入 2=支出；amount 必须 > 0
   */
  @PostMapping
  public Result<TransactionDTO> create(@Valid @RequestBody TransactionRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    // → TransactionService.create()：校验账户状态 + 插入数据库
    TransactionDTO transaction = transactionService.create(userId, request);
    return Result.success(transaction, "记录创建成功");
  }

  /**
   * 更新交易记录接口
   *
   * 流程：校验记录归属权 → 转账记录仅允许修改备注 → 更新 transaction 表
   *
   * @param id 交易记录 ID（URL 路径参数）
   * @param request 交易记录更新请求体
   * @param httpRequest HTTP 请求
   * @return Result<TransactionDTO> 更新后的交易记录
   *
   * 被前端 TransactionListPage.vue 编辑弹窗调用
   * 注意：转账产生的记录（transferId 非空）仅允许修改 note 字段
   */
  @PutMapping("/{id}")
  public Result<TransactionDTO> update(@PathVariable Long id,
      @Valid @RequestBody TransactionRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    // → TransactionService.update()：校验归属权 + 转账记录限制 + 更新数据库
    TransactionDTO transaction = transactionService.update(userId, id, request);
    return Result.success(transaction, "记录更新成功");
  }

  /**
   * 转账接口（PRD P1-5 账户间转账）
   *
   * 流程：校验转出/转入账户不同 → 校验转出账户余额充足
   *     → @Transactional 事务保护 → 生成两条关联记录（转出支出 + 转入收入）
   *     → 共享 transferId（UUID）标识关联关系
   *
   * @param request 转账请求体（fromAccountId/toAccountId/amount/note，含 @Valid 校验）
   * @param httpRequest HTTP 请求
   * @return Result<TransferDTO> 转账结果（含 transferId + 两条记录信息）
   *
   * 被前端 TransferPage.vue 调用
   * 业务异常码：1004 = 余额不足 / 1006 = 不能转账给自己
   */
  @PostMapping("/transfer")
  public Result<TransferDTO> transfer(@Valid @RequestBody TransferRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    // → TransactionService.transfer()：
    //   1. 校验 fromAccountId != toAccountId
    //   2. 校验转出账户余额 ≥ 转账金额
    //   3. @Transactional：生成 transferId(UUID) → 插入转出记录(type=2) → 插入转入记录(type=1)
    TransferDTO transfer = transactionService.transfer(userId, request);
    return Result.success(transfer, "转账成功");
  }

  /**
   * CSV 批量导入接口（PRD P2-3）
   *
   * 流程：校验文件大小（≤5MB）→ 校验文件类型（.csv）→ 解析 CSV 内容
   *     → 校验记录数（≤1000 条）→ 逐条校验 + 插入 → 返回导入结果统计
   *
   * @param file      CSV 文件（multipart/form-data）
   * @param accountId 目标账户 ID（所有导入记录归属此账户）
   * @param httpRequest HTTP 请求
   * @return Result<String> 导入结果（如"成功导入 50 条，失败 2 条"）
   *
   * 被前端 ImportPage.vue 调用
   * CSV 格式：type,categoryName,amount,note,time
   * 业务异常码：1007 = 文件过大 / 1008 = 记录数超限
   */
  @PostMapping("/import")
  public Result<String> importCsv(@RequestParam("file") MultipartFile file,
      @RequestParam("accountId") Long accountId,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    // → TransactionService.importCsv()：
    //   1. 校验文件 ≤ 5MB + .csv 后缀
    //   2. OpenCSV 解析 → 逐条校验（分类名匹配、金额格式、时间格式）
    //   3. 批量插入 transaction 表（≤1000 条）
    String result = transactionService.importCsv(userId, file, accountId);
    return Result.success(result);
  }
}
