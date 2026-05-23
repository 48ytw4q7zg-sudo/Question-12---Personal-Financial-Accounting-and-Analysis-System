package com.example.finance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.finance.common.Result;
import com.example.finance.entity.dto.ImportResultDTO;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.entity.dto.TransactionRequest;
import com.example.finance.entity.dto.TransferDTO;
import com.example.finance.entity.dto.TransferRequest;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 交易记录控制器（PRD P0-4 收支记录 + P1-1 多条件筛选 + P1-5 转账 + P2-3 CSV 批量导入）
 *
 * 职责：接收收支记录的 HTTP 请求，参数校验后转发 TransactionService 处理
 * 路由前缀：/api/v1/transaction
 * 依赖：→ TransactionService（业务逻辑层）→ TransactionMapper + AccountMapper（数据访问层）
 *
 * 接口清单：
 *   GET    /api/transaction              — 查询交易记录（分页 + 多条件筛选）
 *   POST   /api/transaction              — 创建收支记录（记一笔）
 *   PUT    /api/transaction/{id}         — 更新收支记录
 *   DELETE /api/v1/transaction/{id}         — 删除收支记录（转账记录禁止删除）
 *   POST   /api/transaction/transfer     — 转账（生成两条关联记录）
 *   POST   /api/transaction/import       — CSV 批量导入（≤5MB，≤1000 条）
 *
 * 被前端调用：→ api/transaction.js 的 getTransactionList/create/update/transfer/importCsv
 * 被 TransactionListPage.vue、TransferPage.vue、ImportPage.vue 调用
 */
@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
@Validated
public class TransactionController {

  /** → TransactionService：处理收支记录 CRUD + 转账 + CSV 导入的业务逻辑 */
  private final TransactionService transactionService;

  /**
   * 查询交易记录接口（分页 + 多条件筛选 · PRD P0-4 + P1-1）
   *
   * 流程：从 JWT 提取 userId → 转发 TransactionService 处理筛选+分页
   *     sortBy 白名单校验 + 时间范围跨度校验在 Service 层执行（Controller 不含业务逻辑）
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
   */
  @GetMapping
  public Result<IPage<TransactionDTO>> list(
      @RequestParam(required = false) @Min(1) Long accountId,  // 账户ID校验（>=1）
      @RequestParam(required = false) @Min(1) Long categoryId,  // 分类ID校验（>=1）
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false, defaultValue = "time") String sortBy,
      @RequestParam(defaultValue = "1") @Min(1) int pageNum,  // 页码校验（>=1）
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,  // 每页条数校验（1-100）
      HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → TransactionService.list()：sortBy 白名单校验 + pageSize 上限保护 + 时间范围校验 + 分页
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
    Long userId = LoginInterceptor.getUserId(httpRequest);
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
  public Result<TransactionDTO> update(@PathVariable @Min(1) Long id,
      @Valid @RequestBody TransactionRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → TransactionService.update()：校验归属权 + 转账记录限制 + 更新数据库
    TransactionDTO transaction = transactionService.update(userId, id, request);
    return Result.success(transaction, "记录更新成功");
  }

  /**
   * 删除交易记录接口（PRD P0-4）
   *
   * 流程：校验记录归属权 → 转账记录禁止删除 → 物理删除
   *
   * @param id 交易记录 ID（URL 路径参数）
   * @param httpRequest HTTP 请求
   * @return Result<Void> 删除结果
   *
   * 被前端 TransactionListPage.vue 删除按钮调用
   * 注意：转账产生的记录（transferId 非空）禁止删除，避免破坏转账配对完整性
   */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable @Min(1) Long id, HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → TransactionService.delete()：校验归属权 + 转账记录限制 + 删除数据库记录
    transactionService.delete(userId, id);
    return Result.success(null, "记录已删除");
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
   * 业务异常码：3009 = 余额不足 / 3008 = 转出转入账户不可相同
   */
  @PostMapping("/transfer")
  public Result<TransferDTO> transfer(@Valid @RequestBody TransferRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
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
   *     → 校验记录数（≤1000 条）→ 逐条校验 + 插入 → 返回结构化导入结果
   *
   * @param file      CSV 文件（multipart/form-data）
   * @param accountId 目标账户 ID（所有导入记录归属此账户）
   * @param httpRequest HTTP 请求
   * @return Result<ImportResultDTO> 结构化导入结果（成功/失败条数 + 失败明细行号+原因）
   *
   * 被前端 ImportPage.vue 调用，展示导入结果表格 + 失败详情
   * CSV 格式：time,categoryId,type,amount,note
   * 业务异常码：3001 = 文件过大 / 格式错误 / 记录数超限
   */
  @PostMapping("/import")
  public Result<ImportResultDTO> importCsv(@RequestParam("file") MultipartFile file,
      @RequestParam("accountId") @NotNull @Min(1) Long accountId,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → TransactionService.importCsv()：
    //   1. 校验文件 ≤ 5MB + .csv 后缀
    //   2. OpenCSV 解析 → 逐条校验（分类ID、金额、时间格式）
    //   3. 批量插入 transaction 表（≤1000 条）
    //   4. 返回 ImportResultDTO（successCount + failCount + failRows）
    ImportResultDTO result = transactionService.importCsv(userId, file, accountId);
    return Result.success(result);
  }
}
