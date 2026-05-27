package com.example.finance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.finance.common.Result;
import com.example.finance.entity.dto.ImportResultDTO;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.entity.dto.TransactionRequest;
import com.example.finance.entity.dto.TransferDTO;
import com.example.finance.entity.dto.TransferRequest;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.TransactionImportService;
import com.example.finance.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
 *   GET    /api/v1/transaction              — 查询交易记录（分页 + 多条件筛选）
 *   POST   /api/v1/transaction              — 创建收支记录（记一笔）
 *   PUT    /api/v1/transaction/{id}         — 更新收支记录
 *   DELETE /api/v1/transaction/{id}         — 删除收支记录（转账记录禁止删除）
 *   POST   /api/v1/transaction/transfer     — 转账（生成两条关联记录）
 *   POST   /api/v1/transaction/import       — CSV 批量导入（≤5MB，≤1000 条）
 *
 * 被前端调用：→ api/transaction.js 的 getTransactionList/create/update/transfer/importCsv
 * 被 TransactionListPage.vue、TransferPage.vue、ImportPage.vue 调用
 */
@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
@Validated
public class TransactionController {

  /** → TransactionService：处理收支记录 CRUD + 转账的业务逻辑 */
  private final TransactionService transactionService;
  /** → TransactionImportService：处理 CSV 批量导入（从 TransactionService 拆分） */
  private final TransactionImportService transactionImportService;

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
      @RequestParam(required = false) @Size(max = 100) String keyword,  // 防 DoS：限制关键字长度不超过 100 字符（避免超大 LIKE 查询）
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
   * <p>流程：校验转出/转入账户不同 → 死锁预防按 ID 升序加锁 → 校验转出账户余额充足
   *     → @Transactional 事务保护（InnoDB REPEATABLE READ）→ 生成两条关联记录（转出支出 + 转入收入）
   *     → 共享 transferId（UUID）标识关联关系。</p>
   *
   * <p>并发安全设计:</p>
   * <ul>
   *   <li>死锁预防：按账户 ID 升序加锁（先锁小 ID 后锁大 ID），消除 A→B 与 B→A 并发转账的循环等待</li>
   *   <li>余额校验：FOR UPDATE 悲观锁 + 事务内快照一致性防 TOCTOU 并发透支</li>
   *   <li>转账分类：统一使用"其他"支出分类（categoryId 从 category 表运行时查询 · 不依赖种子数据顺序）</li>
   * </ul>
   *
   * <p>调用链: 前端 TransferPage.vue → api/transaction.js transfer() → TransactionController.transfer() → TransactionService.transfer()
   *   → AccountMapper.selectByIdForUpdate() + TransactionMapper.selectAccountIncome/Expense() + TransactionMapper.insert() × 2</p>
   *
   * @param request 转账请求体（fromAccountId/toAccountId/amount/note，含 @Valid 校验 · entity/dto/TransferRequest.java）
   * @param httpRequest HTTP 请求（LoginInterceptor 已注入 userId 属性）
   * @return Result&lt;TransferDTO&gt; 转账结果（含 transferId + 两条关联记录 outRecord/inRecord）
   *
   * 被前端 TransferPage.vue 调用 → api/transaction.js 的 transfer()
   * 业务异常码：3009 = 余额不足（INSUFFICIENT_BALANCE）/ 3008 = 转出转入账户不可相同（SAME_TRANSFER_ACCOUNT）/ 3004 = 账户不存在或已禁用
   */
  @PostMapping("/transfer")
  public Result<TransferDTO> transfer(@Valid @RequestBody TransferRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → service/impl/TransactionServiceImpl.java transfer()：
    //   1. 校验金额非空+正数 + fromAccountId != toAccountId
    //   2. 死锁预防：按 ID 升序确定加锁顺序（先小后大）
    //   3. AccountMapper.selectByIdForUpdate() FOR UPDATE 悲观锁
    //   4. 校验账户归属+status=1 + 余额充足（初始+收入-支出 ≥ 金额）
    //   5. 查询"其他"支出分类 ID（category 表 + type=1 过滤）
    //   6. @Transactional：生成 transferId(UUID) → INSERT 转出(type=EXPENSE) → INSERT 转入(type=INCOME)
    TransferDTO transfer = transactionService.transfer(userId, request);
    return Result.success(transfer, "转账成功");
  }

  /**
   * CSV 批量导入接口（PRD P2-3 · 银行流水/外部数据批量导入）
   *
   * <p>流程：校验文件大小（≤5MB）→ 校验文件类型（.csv）→ 校验账户归属 → OpenCSV 解析
   *     → 分类缓存预加载 → 逐行校验（分类ID/类型/金额/时间格式）+ 逐行 INSERT
   *     → 记录数上限拦截（≤1000 条）→ 返回结构化导入结果（成功条数 + 失败明细行号+原因）。</p>
   *
   * <p>容错机制:</p>
   * <ul>
   *   <li>单行解析失败不影响其他行（failRows 收录行号+原因）</li>
   *   <li>CSV 读取 I/O 异常 → 整个事务回滚</li>
   *   <li>数据库异常（死锁/外键冲突/连接断开）→ 整个事务回滚</li>
   *   <li>文件格式/大小/条数 → 提前拒绝，不进入解析</li>
   * </ul>
   *
   * <p>调用链: 前端 ImportPage.vue → api/transaction.js importCsv() → TransactionController.importCsv()
   *   → TransactionService.importCsv() → EntityValidator + CategoryMapper selectList + OpenCSV CSVReader + TransactionMapper.insert()</p>
   *
   * @param file      CSV 文件（multipart/form-data · UTF-8 编码 · 表头行: time,categoryId,type,amount,note）
   * @param accountId 目标账户 ID（所有导入记录归属此账户 · @NotNull + @Min(1) 校验）
   * @param httpRequest HTTP 请求（LoginInterceptor 已注入 userId 属性）
   * @return Result&lt;ImportResultDTO&gt; 结构化导入结果（successCount + failCount + failRows 明细含行号+原因）
   *
   * 被前端 ImportPage.vue 调用 → api/transaction.js 的 importCsv()
   * 业务异常码：3010 = 文件大小超过5MB（FILE_TOO_LARGE）/ 3002 = 仅支持CSV格式（CSV_FORMAT_ONLY）/ 3012 = 导入条数超限 / 3004 = 账户不存在
   */
  @PostMapping("/import")
  public Result<ImportResultDTO> importCsv(@RequestParam("file") MultipartFile file,
      @RequestParam("accountId") @NotNull @Min(1) Long accountId,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → service/impl/TransactionServiceImpl.java importCsv()：
    //   1. EntityValidator.validateAccount() 校验账户归属
    //   2. 校验文件 ≤ 5MB + .csv 后缀
    //   3. CategoryMapper.selectList() 全量分类缓存到 Map<Long, Category>
    //   4. OpenCSV CSVReader 逐行读取 → 跳过表头 → 逐列解析（time/categoryId/type/amount/note）
    //   5. 逐行语义校验：分类存在 + type=1/2 + amount>0 + 时间格式 yyyy-MM-dd HH:mm:ss + ≤1000条
    //   6. 通过校验的行 → TransactionMapper.insert() 逐行写入 transaction 表
    //   7. 失败行收录到 failRows（行号+原因）→ 不触发回滚（容错）
    //   8. 返回 ImportResultDTO（successCount + failCount + failRows 明细）
    ImportResultDTO result = transactionImportService.importCsv(userId, file, accountId);
    return Result.success(result);
  }
}
