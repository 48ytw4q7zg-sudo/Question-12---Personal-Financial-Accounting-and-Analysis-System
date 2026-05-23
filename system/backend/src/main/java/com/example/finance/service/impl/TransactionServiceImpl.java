package com.example.finance.service.impl;

import java.math.RoundingMode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.EntityValidator;
import com.example.finance.common.enums.Status;
import com.example.finance.common.enums.TransactionType;
import com.example.finance.entity.Account;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.dto.ImportResultDTO;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.entity.dto.TransactionRequest;
import com.example.finance.entity.dto.TransferDTO;
import com.example.finance.entity.dto.TransferRequest;
import com.example.finance.mapper.AccountMapper;
import java.util.Objects;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 交易记录服务实现
 *
 * <p>对应 PRD 功能:</p>
 * <ul>
 *   <li>P0-4 收支记录: 记一笔(create) + 修改(update) + 分页列表(list)</li>
 *   <li>P1-1 多条件筛选: list() 支持时间/账户/分类/关键词组合筛选</li>
 *   <li>P1-5 转账: transfer() 生成一出一进两条关联记录, @Transactional 保证原子性</li>
 *   <li>P2-3 CSV导入: importCsv() 解析银行CSV批量导入, 上限5MB/1000条</li>
 * </ul>
 *
 * <p>关键业务规则:</p>
 * <ul>
 *   <li>转账记录(transferId非空)禁止修改金额, 仅允许修改备注 (PRD P0-4 异常流程②)</li>
 *   <li>转账使用 @Transactional 包裹余额检查+两条INSERT, 利用InnoDB REPEATABLE READ防并发透支</li>
 *   <li>CSV导入使用 @Transactional 包裹批量插入, 整批原子提交(全部成功或全部回滚)</li>
 *   <li>所有操作强制校验 user_id 归属, 确保数据隔离</li>
 * </ul>
 *
 * <p>调用方: TransactionController (controller/TransactionController.java)</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

  /** sortBy 参数白名单：只允许 time/amount_asc/amount_desc（与前端筛选下拉选项对齐），防止非法排序注入 */
  private static final Set<String> ALLOWED_SORT_BY = Set.of("time", "amount_asc", "amount_desc");
  /** 转账默认分类名称（"其他"，运行时查询 category 表获取 ID，不依赖种子数据顺序） */
  private static final String TRANSFER_CATEGORY_NAME = "其他";
  private static final String TRANSFER_OUT_SUFFIX = "(转出)";
  private static final String TRANSFER_IN_SUFFIX = "(转入)";
  private static final String TRANSFER_ARROW = " → ";

  // CSV 列索引常量（列顺序：日期,分类ID,类型,金额,备注 —— 与前端导入模板对齐）
  private static final int CSV_COL_TIME = 0;         // 第1列：日期(yyyy-MM-dd HH:mm:ss)
  private static final int CSV_COL_CATEGORY_ID = 1;  // 第2列：分类ID
  private static final int CSV_COL_TYPE = 2;         // 第3列：交易类型(1=收入/2=支出)
  private static final int CSV_COL_AMOUNT = 3;       // 第4列：金额
  private static final int CSV_COL_NOTE = 4;         // 第5列：备注(可选)
  private static final int CSV_MIN_COLUMNS = 4;
  /** 时间格式化常量（复用避免重复创建 DateTimeFormatter） */
  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  // PRD P2-3: 文件大小上限 5MB
  private static final long CSV_MAX_FILE_SIZE = 5 * 1024 * 1024;
  // PRD P2-3: 单次导入上限 1000 条
  private static final int CSV_MAX_RECORDS = 1000;
  // 分页大小上限保护：防止一次查询海量数据导致 OOM
  private static final int MAX_PAGE_SIZE = 100;

  /** -> TransactionMapper：交易记录 CRUD + 统计聚合查询数据访问 */
  private final TransactionMapper transactionMapper;
  /** -> AccountMapper：余额查询 + 悲观锁(for update) + toDTO 关联名称填充 */
  private final AccountMapper accountMapper;
  /** -> CategoryMapper：转账分类名查询 + CSV导入分类缓存 + toDTO 关联名称填充 */
  private final CategoryMapper categoryMapper;
  /** -> EntityValidator：跨 Service 共享的 validateAccount/validateCategory 校验 */
  private final EntityValidator entityValidator;

  /**
   * 查询交易记录（分页 + 多条件筛选）
   *
   * <p>对应 PRD P0-4(分页列表) + P1-1(多条件筛选)。</p>
   * <p>使用 MyBatis XML 动态 SQL 拼接条件, RowBounds 物理分页。</p>
   * <p>筛选条件: accountId / categoryId / startTime / endTime / keyword(模糊匹配备注) / sortBy。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param accountId 账户ID筛选(空=不过滤)
   * @param categoryId 分类ID筛选(空=不过滤)
   * @param startTime 起始时间(yyyy-MM-dd HH:mm:ss, 空=不过滤)
   * @param endTime 结束时间(yyyy-MM-dd HH:mm:ss, 空=不过滤)
   * @param keyword 关键词(模糊匹配备注, 空=不过滤)
   * @param sortBy 排序字段(白名单: id/time/create_time, 默认time)
   * @param pageNum 页码(从1开始)
   * @param pageSize 每页条数(默认10)
   * @return 分页结果(含total和records)
   */
  @Override
  @Transactional(readOnly = true)
  public IPage<TransactionDTO> list(Long userId, Long accountId, Long categoryId,
      String startTime, String endTime, String keyword, String sortBy,
      int pageNum, int pageSize) {
    // pageSize 上限保护：防止一次查询海量数据导致 OOM（业务逻辑，应由 Service 层处理）
    pageSize = Math.min(pageSize, MAX_PAGE_SIZE);  // 限制每页最大条数防OOM
    // sortBy 白名单校验：防止非法排序字段注入（业务逻辑，应由 Service 层处理）
    if (!ALLOWED_SORT_BY.contains(sortBy)) {  // 排序参数不在白名单内
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), String.format("%s: 排序参数只能是time/amount_asc/amount_desc", ErrorCode.PARAM_INVALID.getMsg()));  // 抛出参数非法异常
    }
    // PRD P1-1: 时间范围最大跨度1年（防止全表扫描）
    if (startTime != null && endTime != null) {  // 两个时间都传了才校验跨度
      try {
        LocalDateTime start = LocalDateTime.parse(startTime, DTF);  // 解析起始时间
        LocalDateTime end = LocalDateTime.parse(endTime, DTF);  // 解析结束时间
        if (end.minusYears(1).isAfter(start)) {  // 时间跨度超过1年
          throw new BusinessException(ErrorCode.TIME_RANGE_TOO_LARGE.getCode(), ErrorCode.TIME_RANGE_TOO_LARGE.getMsg());  // 拒绝超大时间范围
        }
      } catch (java.time.format.DateTimeParseException e) {  // 时间格式解析失败
        throw new BusinessException(ErrorCode.TIME_FORMAT_INVALID.getCode(), ErrorCode.TIME_FORMAT_INVALID.getMsg());  // 提示格式错误
      }
    }
    // R-05-issue-4: 已修复 - RowBounds+独立count是XML动态ORDER BY的标准MyBatis分页模式,Page对象正确封装total/records
    // LIKE 通配符转义：防止用户输入的 % 和 _ 被解释为 LIKE 通配符
    String escapedKeyword = escapeLikeKeyword(keyword);  // 转义LIKE通配符
    Page<TransactionDTO> page = new Page<>(pageNum, pageSize);  // 创建分页对象
    List<TransactionDTO> records = transactionMapper.selectTransactionList(  // 查询交易列表(含分页)
        userId, accountId, categoryId, startTime, endTime, escapedKeyword, sortBy,  // 查询参数
        new org.apache.ibatis.session.RowBounds((pageNum - 1) * pageSize, pageSize)  // RowBounds物理分页
    );
    Long total = transactionMapper.selectTransactionCount(  // 查询总记录数(不含分页)
        userId, accountId, categoryId, startTime, endTime, escapedKeyword  // 查询参数
    );
    page.setRecords(records);  // 设置分页记录列表
    page.setTotal(total != null ? total : 0);  // 设置总记录数(null保护)
    return page;  // 返回分页结果
  }

  /**
   * 创建交易记录（记一笔）
   *
   * <p>对应 PRD P0-4 主流程: 用户填写金额/类型/分类/账户/时间/备注, 系统创建收支记录。</p>
   * <p>前置校验: 账户归属当前用户且status=1(活跃), 分类存在。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param request 交易请求参数(含accountId/categoryId/type/amount/note/time)
   * @return 创建后的交易记录DTO(含关联的账户名/分类名)
   * @throws BusinessException 3004=账户不存在或已禁用 / 3005=分类不存在
   */
  @Override
  @Transactional
  public TransactionDTO create(Long userId, TransactionRequest request) {
    // 校验账户归属并复用对象
    Account account = entityValidator.validateAccount(userId, request.getAccountId());
    // 校验分类存在并复用对象
    Category category = entityValidator.validateCategory(request.getCategoryId());

    Transaction transaction = new Transaction();  // 创建交易记录实体
    transaction.setUserId(userId);  // 设置用户ID
    transaction.setAccountId(request.getAccountId());  // 设置账户ID
    transaction.setCategoryId(request.getCategoryId());  // 设置分类ID
    transaction.setType(request.getType());  // 设置交易类型(1=收入/2=支出)
    transaction.setAmount(request.getAmount());  // 设置金额
    transaction.setNote(request.getNote());  // 设置备注
    transaction.setTime(request.getTime());  // 设置交易时间
    transaction.setCreateTime(LocalDateTime.now());  // 设置创建时间
    transaction.setUpdateTime(LocalDateTime.now());  // 设置更新时间

    transactionMapper.insert(transaction);  // 插入数据库
    return toDTO(transaction, account, category);  // 转为DTO返回
  }

  /**
   * 更新交易记录
   *
   * <p>对应 PRD P0-4 修改操作。</p>
   * <p>关键约束: 转账记录(transferId非空)禁止修改金额, 仅允许修改备注 (PRD P0-4 异常流程②)。</p>
   * <p>普通交易记录允许更新全部字段(accountId/categoryId/type/amount/note/time)。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param transactionId 交易记录ID
   * @param request 更新请求参数
   * @return 更新后的交易记录DTO
   * @throws BusinessException 3006=转账记录金额不可修改 / 3011=收支记录不存在
   */
  @Override
  @Transactional
  public TransactionDTO update(Long userId, Long transactionId, TransactionRequest request) {
    Transaction transaction = getTransactionById(userId, transactionId);  // 查询并校验归属

    // 转账记录仅允许修改备注
    if (transaction.getTransferId() != null) {  // 是转账记录
      // 转账记录禁止修改金额：仅当request.getAmount()非null且与原始金额不同时才抛异常
      // 说明：request.getAmount()为null时表示前端只修改备注，金额不变，不应抛异常
      if (request.getAmount() != null && transaction.getAmount().compareTo(request.getAmount()) != 0) {  // request金额非null且与原始金额不同
        throw new BusinessException(ErrorCode.TRANSFER_RECORD_NOT_MODIFIABLE.getCode(), ErrorCode.TRANSFER_RECORD_NOT_MODIFIABLE.getMsg());  // 抛出业务异常
      }
      String newNote = request.getNote() != null ? request.getNote() : "";  // 新备注(null→空字符串)
      String oldNote = transaction.getNote() != null ? transaction.getNote() : "";  // 旧备注(null→空字符串)
      if (!oldNote.equals(newNote)) {  // 备注有变化才更新
        transaction.setNote(request.getNote());  // 更新备注
        transaction.setUpdateTime(LocalDateTime.now());  // 更新修改时间
        transactionMapper.updateById(transaction);  // 写入数据库
      }
      // 预加载关联对象，避免toDTO单参数版的2次额外DB查询
      Account transferAccount = accountMapper.selectById(transaction.getAccountId());  // 查询关联账户
      Category transferCategory = categoryMapper.selectById(transaction.getCategoryId());  // 查询关联分类
      return toDTO(transaction, transferAccount, transferCategory);  // 转为DTO返回
    }

    // 普通交易记录允许更新全部字段，重新校验账户归属和分类存在，复用对象
    Account account = entityValidator.validateAccount(userId, request.getAccountId());  // 校验账户归属
    Category category = entityValidator.validateCategory(request.getCategoryId());  // 校验分类存在
    transaction.setAccountId(request.getAccountId());  // 更新账户ID
    transaction.setCategoryId(request.getCategoryId());  // 更新分类ID
    transaction.setType(request.getType());  // 更新交易类型
    transaction.setAmount(request.getAmount());  // 更新金额
    transaction.setNote(request.getNote());  // 更新备注
    transaction.setTime(request.getTime());  // 更新交易时间
    transaction.setUpdateTime(LocalDateTime.now());  // 更新修改时间

    transactionMapper.updateById(transaction);  // 写入数据库
    return toDTO(transaction, account, category);  // 转为DTO返回
  }

  /**
   * 删除交易记录
   *
   * <p>对应 PRD P0-4 删除操作。</p>
   * <p>关键约束: 转账关联记录(transferId非空)禁止删除, 避免破坏转账配对完整性。</p>
   * <p>普通交易记录直接物理删除。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param transactionId 交易记录ID
   * @throws BusinessException 3007=转账记录不可删除 / 3011=收支记录不存在
   */
  @Override
  @Transactional
  public void delete(Long userId, Long transactionId) {
    Transaction transaction = getTransactionById(userId, transactionId);  // 查询并校验归属

    // 转账记录禁止删除（破坏一出一进配对会导致余额统计错误）
    if (transaction.getTransferId() != null) {  // 是转账记录
      throw new BusinessException(ErrorCode.TRANSFER_RECORD_NOT_DELETABLE.getCode(), ErrorCode.TRANSFER_RECORD_NOT_DELETABLE.getMsg());  // 抛出业务异常
    }

    transactionMapper.deleteById(transactionId);  // 物理删除交易记录
  }

  /**
   * 转账（在两个账户间生成一出一进两条关联记录）
   *
   * <p>对应 PRD P1-5 转账功能。</p>
   * <p>业务流程:</p>
   * <ol>
   *   <li>校验转出/转入账户归属且status=1</li>
   *   <li>校验转出账户 ≠ 转入账户</li>
   *   <li>在 @Transactional 事务内: 检查转出账户余额充足 → 生成UUID作为transferId → INSERT转出(支出) → INSERT转入(收入)</li>
   * </ol>
   * <p>并发安全: 利用InnoDB REPEATABLE READ隔离级别, 事务内SELECT余额和INSERT共享同一快照, 防并发透支。</p>
   * <p>教学简化: 不做后端幂等, 前端通过按钮loading状态防连点。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param request 转账请求参数(含fromAccountId/toAccountId/amount/note)
   * @return 转账结果DTO(含transferId和两条关联记录)
   * @throws BusinessException 3008=转出转入账户不可相同 / 3009=余额不足 / 3004=账户不存在或已禁用
   */
  @Override
  @Transactional
  public TransferDTO transfer(Long userId, TransferRequest request) {
    // 校验转出和转入账户归属（两账户均加悲观锁防并发透支/禁用 TOCTOU）
    Account fromAccount = accountMapper.selectByIdForUpdate(request.getFromAccountId());  // 悲观锁查询转出账户
    // 校验：账户存在 + 归属当前用户 + 状态为活跃（Integer用Objects.equals比较值，避免引用比较bug）
    if (fromAccount == null || !Objects.equals(fromAccount.getUserId(), userId) || !Objects.equals(fromAccount.getStatus(), Status.ACTIVE.getValue())) {
      throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getCode(), ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getMsg());  // 抛出业务异常
    }
    Account toAccount = accountMapper.selectByIdForUpdate(request.getToAccountId());  // 悲观锁查询转入账户
    // 校验：账户存在 + 归属当前用户 + 状态为活跃（Integer用Objects.equals比较值，避免引用比较bug）
    if (toAccount == null || !Objects.equals(toAccount.getUserId(), userId) || !Objects.equals(toAccount.getStatus(), Status.ACTIVE.getValue())) {
      throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getCode(), ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getMsg());  // 抛出业务异常
    }

    // 校验不能转给自己
    if (request.getFromAccountId().equals(request.getToAccountId())) {  // 转出和转入相同
      throw new BusinessException(ErrorCode.SAME_TRANSFER_ACCOUNT.getCode(), ErrorCode.SAME_TRANSFER_ACCOUNT.getMsg());  // 抛出业务异常
    }

    // 校验转出账户余额充足
    BigDecimal totalIncome = transactionMapper.selectAccountIncome(userId, fromAccount.getId());  // 查询转出账户总收入
    BigDecimal totalExpense = transactionMapper.selectAccountExpense(userId, fromAccount.getId());  // 查询转出账户总支出
    BigDecimal currentBalance = fromAccount.getInitialBalance().add(totalIncome).subtract(totalExpense);  // 计算当前余额
    if (currentBalance.compareTo(request.getAmount()) < 0) {  // 余额不足
      throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE.getCode(), ErrorCode.INSUFFICIENT_BALANCE.getMsg());  // 抛出业务异常
    }

    // 查询"其他"分类ID（运行时查询，不依赖种子数据顺序）
    Category transferCategory = categoryMapper.selectOne(  // 查询"其他"分类
        new LambdaQueryWrapper<Category>().eq(Category::getName, TRANSFER_CATEGORY_NAME)  // 按名称查询
    );
    if (transferCategory == null) {  // "其他"分类不存在
      throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND.getCode(), ErrorCode.CATEGORY_NOT_FOUND.getMsg() + "：未找到「其他」分类，请检查种子数据");  // 抛出业务异常
    }
    Long transferCategoryId = transferCategory.getId();  // 获取"其他"分类ID

    // 生成转账关联ID（UUID）
    String transferId = UUID.randomUUID().toString();  // 生成UUID作为转账关联ID

    LocalDateTime now = LocalDateTime.now();  // 当前时间
    String note = request.getNote() != null ? request.getNote() : "";  // 备注(null→空字符串)
    // R-05-issue-1: 已修复 - outNote方向修正为fromAccount→toAccount(转出)
    String outNote = fromAccount.getName() + TRANSFER_ARROW + toAccount.getName() + TRANSFER_OUT_SUFFIX + (note.isEmpty() ? "" : ": " + note);
    String inNote = fromAccount.getName() + TRANSFER_ARROW + toAccount.getName() + TRANSFER_IN_SUFFIX + (note.isEmpty() ? "" : ": " + note);

    // 创建转出记录（type=支出，category=其他）
    Transaction outTransaction = new Transaction();  // 创建转出交易实体
    outTransaction.setUserId(userId);  // 设置用户ID
    outTransaction.setAccountId(request.getFromAccountId());  // 设置转出账户ID
    outTransaction.setCategoryId(transferCategoryId);  // 设置"其他"分类ID
    outTransaction.setType(TransactionType.EXPENSE.getValue());  // 设置类型为支出
    outTransaction.setAmount(request.getAmount());  // 设置金额
    outTransaction.setNote(outNote);  // 设置转出备注
    outTransaction.setTime(now);  // 设置交易时间
    outTransaction.setTransferId(transferId);  // 设置转账关联ID
    outTransaction.setCreateTime(now);  // 设置创建时间
    outTransaction.setUpdateTime(now);  // 设置更新时间
    transactionMapper.insert(outTransaction);  // 插入转出记录

    // 创建转入记录（type=收入，category=其他）
    Transaction inTransaction = new Transaction();  // 创建转入交易实体
    inTransaction.setUserId(userId);  // 设置用户ID
    inTransaction.setAccountId(request.getToAccountId());  // 设置转入账户ID
    inTransaction.setCategoryId(transferCategoryId);  // 设置"其他"分类ID
    inTransaction.setType(TransactionType.INCOME.getValue());  // 设置类型为收入
    inTransaction.setAmount(request.getAmount());  // 设置金额
    inTransaction.setNote(inNote);  // 设置转入备注
    inTransaction.setTime(now);  // 设置交易时间
    inTransaction.setTransferId(transferId);  // 设置转账关联ID
    inTransaction.setCreateTime(now);  // 设置创建时间
    inTransaction.setUpdateTime(now);  // 设置更新时间
    transactionMapper.insert(inTransaction);  // 插入转入记录

    // 组装返回结果（复用已加载的 fromAccount/toAccount/transferCategory，消除4次额外DB查询）
    TransferDTO dto = new TransferDTO();  // 创建转账结果DTO
    dto.setTransferId(transferId);  // 设置转账关联ID
    dto.setOutRecord(toDTO(outTransaction, fromAccount, transferCategory));  // 设置转出记录DTO
    dto.setInRecord(toDTO(inTransaction, toAccount, transferCategory));  // 设置转入记录DTO
    return dto;  // 返回转账结果
  }

  /**
   * 批量导入 CSV 文件（银行流水导入 · P2-3）
   *
   * <p>对应 PRD P2-3 导入银行CSV。</p>
   * <p>CSV格式: 日期(yyyy-MM-dd HH:mm:ss), 分类ID, 类型(1=收入/2=支出), 金额, 备注。</p>
   * <p>限制: 文件≤5MB, 单次≤1000条, 仅支持UTF-8编码的.csv文件。</p>
   * <p>容错: 单行解析失败记录到 failRows(行号+原因), 不影响其他行; 全部在 @Transactional 事务内执行。</p>
   * <p>PRD P2-3 Step 2: "预览导入结果（有效条数 + 错误条数）" → 返回结构化 ImportResultDTO。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param file 上传的CSV文件
   * @param accountId 导入目标账户ID
   * @return 结构化导入结果(成功/失败条数 + 失败明细行号+原因)
   * @throws BusinessException 3010=文件大小超限 / 3002=格式错误 / 3012=单次导入超限
   */
  @Override
  @Transactional
  public ImportResultDTO importCsv(Long userId, MultipartFile file, Long accountId) {
    entityValidator.validateAccount(userId, accountId);  // 校验账户归属

    // PRD P2-3 异常流程①: 文件大小超过 5MB → 拒绝
    if (file.getSize() > CSV_MAX_FILE_SIZE) {  // 文件超过5MB
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE.getCode(), ErrorCode.FILE_TOO_LARGE.getMsg());  // 抛出文件过大异常
    }
    // PRD P2-3 异常流程①: 文件格式非 CSV → 拒绝
    String filename = file.getOriginalFilename();  // 获取原始文件名
    if (filename == null || !filename.toLowerCase().endsWith(".csv")) {  // 文件名非.csv结尾
      throw new BusinessException(ErrorCode.CSV_FORMAT_ONLY.getCode(), ErrorCode.CSV_FORMAT_ONLY.getMsg());  // 抛出格式异常
    }

    ImportResultDTO result = new ImportResultDTO();  // 创建导入结果DTO

    // 预加载所有分类到 Map，避免循环内逐行 selectById（种子数据仅13条，1次查询覆盖全部）
    Map<Long, Category> categoryCache = categoryMapper.selectList(new LambdaQueryWrapper<Category>()).stream()  // 一次查询所有分类
        .collect(Collectors.toMap(Category::getId, c -> c));  // 分类ID→分类对象映射

    try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {  // 创建CSV读取器
      String[] line;  // 每行数据
      boolean firstLine = true;  // 是否首行(表头)
      int rowNumber = 1; // 行号从1开始，表头是第1行

      while ((line = reader.readNext()) != null) {  // 逐行读取
        rowNumber++;  // 行号递增
        // 跳过表头
        if (firstLine) {  // 首行跳过
          firstLine = false;  // 标记已跳过
          continue;  // 继续下一行
        }

        try {  // 单行解析
          // CSV 格式: 日期,分类ID,类型,金额,备注
          if (line.length < CSV_MIN_COLUMNS) {  // 列数不足
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();  // 创建失败行记录
            failRow.setRow(rowNumber);  // 设置行号
            failRow.setReason("列数不足，至少需要4列（日期,分类ID,类型,金额）");  // 设置失败原因
            result.getFailRows().add(failRow);  // 加入失败列表
            continue;  // 继续下一行
          }

          Transaction transaction = new Transaction();  // 创建交易实体
          transaction.setUserId(userId);  // 设置用户ID
          transaction.setAccountId(accountId);  // 设置账户ID
          long categoryId = Long.parseLong(line[CSV_COL_CATEGORY_ID].trim());
          int type = Integer.parseInt(line[CSV_COL_TYPE].trim());
          BigDecimal amount = new BigDecimal(line[CSV_COL_AMOUNT].trim()).setScale(2, RoundingMode.HALF_UP);

          // 语义校验：分类必须存在（从预加载缓存查询，避免逐行DB查询）
          Category category = categoryCache.get(categoryId);  // 从缓存查找分类
          if (category == null) {  // 分类不存在
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();  // 创建失败行记录
            failRow.setRow(rowNumber);  // 设置行号
            failRow.setReason("分类ID " + categoryId + " 不存在");  // 设置失败原因
            result.getFailRows().add(failRow);  // 加入失败列表
            continue;  // 继续下一行
          }
          // 语义校验：类型必须为 1(收入) 或 2(支出)
          if (type != TransactionType.INCOME.getValue() && type != TransactionType.EXPENSE.getValue()) {  // 类型非法
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();  // 创建失败行记录
            failRow.setRow(rowNumber);  // 设置行号
            failRow.setReason("交易类型必须为1(收入)或2(支出)，当前值: " + type);  // 设置失败原因
            result.getFailRows().add(failRow);  // 加入失败列表
            continue;  // 继续下一行
          }
          // 语义校验：金额必须大于 0
          if (amount.compareTo(BigDecimal.ZERO) <= 0) {  // 金额<=0
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();  // 创建失败行记录
            failRow.setRow(rowNumber);  // 设置行号
            failRow.setReason("金额必须大于0，当前值: " + amount);  // 设置失败原因
            result.getFailRows().add(failRow);  // 加入失败列表
            continue;  // 继续下一行
          }

          transaction.setCategoryId(categoryId);  // 设置分类ID
          transaction.setType(type);  // 设置交易类型
          transaction.setAmount(amount);  // 设置金额
          transaction.setNote(line.length > CSV_COL_NOTE ? line[CSV_COL_NOTE].trim() : "");
          transaction.setTime(LocalDateTime.parse(line[CSV_COL_TIME].trim(), DTF));
          transaction.setCreateTime(LocalDateTime.now());  // 设置创建时间
          transaction.setUpdateTime(LocalDateTime.now());  // 设置更新时间

          // PRD P2-3 业务规则①: 单次导入上限 1000 条（超过上限的行记录为失败；单行解析错误不触发回滚，但 CSV 读取异常会回滚全部已插入记录）
          if (result.getSuccessCount() >= CSV_MAX_RECORDS) {  // 已达上限
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();  // 创建失败行记录
            failRow.setRow(rowNumber);  // 设置行号
            failRow.setReason("单次导入不能超过 " + CSV_MAX_RECORDS + " 条记录");  // 设置失败原因
            result.getFailRows().add(failRow);  // 加入失败列表
            continue;  // 继续下一行
          }

          transactionMapper.insert(transaction);  // 插入交易记录到数据库
          result.setSuccessCount(result.getSuccessCount() + 1);  // 成功计数+1
        } catch (NumberFormatException e) {  // 数字格式解析错误
          ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();  // 创建失败行记录
          failRow.setRow(rowNumber);  // 设置行号
          failRow.setReason("数据格式错误: " + e.getMessage());  // 设置失败原因
          result.getFailRows().add(failRow);  // 加入失败列表
        } catch (Exception e) {  // 其他解析错误
          ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();  // 创建失败行记录
          failRow.setRow(rowNumber);  // 设置行号
          failRow.setReason("解析失败: " + e.getMessage());  // 设置失败原因
          result.getFailRows().add(failRow);  // 加入失败列表
        }
      }
    } catch (Exception e) {  // CSV文件读取失败
      log.error("CSV 文件读取失败: {}", e.getMessage(), e);  // 记录错误日志
      throw new BusinessException(ErrorCode.CSV_READ_ERROR.getCode(), ErrorCode.CSV_READ_ERROR.getMsg());  // 抛出CSV读取异常
    }

    result.setFailCount(result.getFailRows().size());  // 统计失败总数
    return result;  // 返回导入结果
  }

  /**
   * LIKE 通配符转义：将用户输入中的 %、_、\ 转义为 \%、\_、\\，
   * 防止这些字符被 MySQL LIKE 解释为通配符而非字面字符
   */
  private String escapeLikeKeyword(String keyword) {  // LIKE通配符转义
    if (keyword == null || keyword.isEmpty()) return keyword;  // null或空字符串直接返回
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");  // 转义反斜杠、百分号、下划线
  }

  /**
   * 根据ID查询交易记录（校验归属）
   */
  private Transaction getTransactionById(Long userId, Long transactionId) {  // 查询交易记录并校验归属
    Transaction transaction = transactionMapper.selectById(transactionId);  // 根据ID查询交易记录
    // 校验：交易存在 + 归属当前用户（Integer用Objects.equals比较值，避免引用比较bug和null userId NPE）
    if (transaction == null || !Objects.equals(transaction.getUserId(), userId)) {
      throw new BusinessException(ErrorCode.RECORD_NOT_FOUND.getCode(), ErrorCode.RECORD_NOT_FOUND.getMsg());  // 抛出业务异常
    }
    return transaction;  // 返回交易实体
  }

  /**
   * Entity → DTO 转换（传入预加载的关联对象，避免重复查询）
   */
  private TransactionDTO toDTO(Transaction transaction, Account account, Category category) {  // Entity→DTO转换
    TransactionDTO dto = new TransactionDTO();  // 创建DTO对象
    dto.setId(transaction.getId());  // 设置ID
    dto.setAccountId(transaction.getAccountId());  // 设置账户ID
    dto.setCategoryId(transaction.getCategoryId());  // 设置分类ID
    dto.setType(transaction.getType());  // 设置交易类型
    dto.setAmount(transaction.getAmount());  // 设置金额
    dto.setNote(transaction.getNote());  // 设置备注
    dto.setTime(transaction.getTime() != null ? transaction.getTime().format(DTF) : null);  // 设置格式化时间(null保护)
    dto.setTransferId(transaction.getTransferId());  // 设置转账关联ID
    dto.setCreateTime(transaction.getCreateTime());  // 设置创建时间
    dto.setUpdateTime(transaction.getUpdateTime());  // 设置更新时间
    if (account != null) dto.setAccountName(account.getName());  // 设置账户名称(非空才设)
    if (category != null) dto.setCategoryName(category.getName());  // 设置分类名称(非空才设)
    return dto;  // 返回DTO
  }
}
