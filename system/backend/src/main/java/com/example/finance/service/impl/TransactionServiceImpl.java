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

  /** sortBy 参数白名单：只允许 time/amount_asc/amount_desc，防止非法排序注入 */
  private static final Set<String> ALLOWED_SORT_BY = Set.of("time", "amount_asc", "amount_desc");
  /** 转账默认分类名称（"其他"，运行时查询 category 表获取 ID，不依赖种子数据顺序） */
  private static final String TRANSFER_CATEGORY_NAME = "其他";
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
  /** -> EntityValidator：跨 Service 共享的 validateAccount/validateAccount 校验 */
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
    pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
    // sortBy 白名单校验：防止非法排序字段注入（业务逻辑，应由 Service 层处理）
    if (!ALLOWED_SORT_BY.contains(sortBy)) {
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), String.format("%s: 排序参数只能是time/amount_asc/amount_desc", ErrorCode.PARAM_INVALID.getMsg()));
    }
    // PRD P1-1: 时间范围最大跨度1年（防止全表扫描）
    if (startTime != null && endTime != null) {
      try {
        LocalDateTime start = LocalDateTime.parse(startTime, DTF);
        LocalDateTime end = LocalDateTime.parse(endTime, DTF);
        if (end.minusYears(1).isAfter(start)) {
          throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "时间范围最大跨度1年");
        }
      } catch (java.time.format.DateTimeParseException e) {
        throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "时间格式须为yyyy-MM-dd HH:mm:ss");
      }
    }
    // R-05-issue-4: 已修复 - RowBounds+独立count是XML动态ORDER BY的标准MyBatis分页模式,Page对象正确封装total/records
    // LIKE 通配符转义：防止用户输入的 % 和 _ 被解释为 LIKE 通配符
    String escapedKeyword = escapeLikeKeyword(keyword);
    Page<TransactionDTO> page = new Page<>(pageNum, pageSize);
    List<TransactionDTO> records = transactionMapper.selectTransactionList(
        userId, accountId, categoryId, startTime, endTime, escapedKeyword, sortBy,
        new org.apache.ibatis.session.RowBounds((pageNum - 1) * pageSize, pageSize)
    );
    Long total = transactionMapper.selectTransactionCount(
        userId, accountId, categoryId, startTime, endTime, escapedKeyword
    );
    page.setRecords(records);
    page.setTotal(total != null ? total : 0);
    return page;
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

    Transaction transaction = new Transaction();
    transaction.setUserId(userId);
    transaction.setAccountId(request.getAccountId());
    transaction.setCategoryId(request.getCategoryId());
    transaction.setType(request.getType());
    transaction.setAmount(request.getAmount());
    transaction.setNote(request.getNote());
    transaction.setTime(request.getTime());
    transaction.setCreateTime(LocalDateTime.now());
    transaction.setUpdateTime(LocalDateTime.now());

    transactionMapper.insert(transaction);
    return toDTO(transaction, account, category);
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
    Transaction transaction = getTransactionById(userId, transactionId);

    // 转账记录仅允许修改备注
    if (transaction.getTransferId() != null) {
      // 转账记录禁止修改金额
      if (transaction.getAmount().compareTo(request.getAmount()) != 0) {
        throw new BusinessException(ErrorCode.TRANSFER_RECORD_NOT_MODIFIABLE.getCode(), ErrorCode.TRANSFER_RECORD_NOT_MODIFIABLE.getMsg());
      }
      String newNote = request.getNote() != null ? request.getNote() : "";
      String oldNote = transaction.getNote() != null ? transaction.getNote() : "";
      if (!oldNote.equals(newNote)) {
        transaction.setNote(request.getNote());
        transaction.setUpdateTime(LocalDateTime.now());
        transactionMapper.updateById(transaction);
      }
      // 预加载关联对象，避免toDTO单参数版的2次额外DB查询
      Account transferAccount = accountMapper.selectById(transaction.getAccountId());
      Category transferCategory = categoryMapper.selectById(transaction.getCategoryId());
      return toDTO(transaction, transferAccount, transferCategory);
    }

    // 普通交易记录允许更新全部字段，重新校验账户归属和分类存在，复用对象
    Account account = entityValidator.validateAccount(userId, request.getAccountId());
    Category category = entityValidator.validateCategory(request.getCategoryId());
    transaction.setAccountId(request.getAccountId());
    transaction.setCategoryId(request.getCategoryId());
    transaction.setType(request.getType());
    transaction.setAmount(request.getAmount());
    transaction.setNote(request.getNote());
    transaction.setTime(request.getTime());
    transaction.setUpdateTime(LocalDateTime.now());

    transactionMapper.updateById(transaction);
    return toDTO(transaction, account, category);
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
    Transaction transaction = getTransactionById(userId, transactionId);

    // 转账记录禁止删除（破坏一出一进配对会导致余额统计错误）
    if (transaction.getTransferId() != null) {
      throw new BusinessException(ErrorCode.TRANSFER_RECORD_NOT_DELETABLE.getCode(), ErrorCode.TRANSFER_RECORD_NOT_DELETABLE.getMsg());
    }

    transactionMapper.deleteById(transactionId);
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
    // 校验转出和转入账户归属（转出账户加悲观锁防并发透支 TOCTOU）
    Account fromAccount = accountMapper.selectByIdForUpdate(request.getFromAccountId());
    if (fromAccount == null || !fromAccount.getUserId().equals(userId) || fromAccount.getStatus() != Status.ACTIVE.getValue()) {
      throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getCode(), ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getMsg());
    }
    Account toAccount = entityValidator.validateAccount(userId, request.getToAccountId());

    // 校验不能转给自己
    if (request.getFromAccountId().equals(request.getToAccountId())) {
      throw new BusinessException(ErrorCode.SAME_TRANSFER_ACCOUNT.getCode(), ErrorCode.SAME_TRANSFER_ACCOUNT.getMsg());
    }

    // 校验转出账户余额充足
    BigDecimal totalIncome = transactionMapper.selectAccountIncome(userId, fromAccount.getId());
    BigDecimal totalExpense = transactionMapper.selectAccountExpense(userId, fromAccount.getId());
    BigDecimal currentBalance = fromAccount.getInitialBalance().add(totalIncome).subtract(totalExpense);
    if (currentBalance.compareTo(request.getAmount()) < 0) {
      throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE.getCode(), ErrorCode.INSUFFICIENT_BALANCE.getMsg());
    }

    // 查询"其他"分类ID（运行时查询，不依赖种子数据顺序）
    Category transferCategory = categoryMapper.selectOne(
        new LambdaQueryWrapper<Category>().eq(Category::getName, TRANSFER_CATEGORY_NAME)
    );
    if (transferCategory == null) {
      throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND.getCode(), ErrorCode.CATEGORY_NOT_FOUND.getMsg() + "：未找到「其他」分类，请检查种子数据");
    }
    Long transferCategoryId = transferCategory.getId();

    // 生成转账关联ID（UUID）
    String transferId = UUID.randomUUID().toString();

    LocalDateTime now = LocalDateTime.now();
    String note = request.getNote() != null ? request.getNote() : "";
    // R-05-issue-1: 已修复 - outNote方向修正为fromAccount→toAccount(转出)
    String outNote = fromAccount.getName() + "→" + toAccount.getName() + "(转出)" + (note.isEmpty() ? "" : ": " + note);
    String inNote = fromAccount.getName() + "→" + toAccount.getName() + "(转入)" + (note.isEmpty() ? "" : ": " + note);

    // 创建转出记录（type=支出，category=其他）
    Transaction outTransaction = new Transaction();
    outTransaction.setUserId(userId);
    outTransaction.setAccountId(request.getFromAccountId());
    outTransaction.setCategoryId(transferCategoryId);
    outTransaction.setType(TransactionType.EXPENSE.getValue());
    outTransaction.setAmount(request.getAmount());
    outTransaction.setNote(outNote);
    outTransaction.setTime(now);
    outTransaction.setTransferId(transferId);
    outTransaction.setCreateTime(now);
    outTransaction.setUpdateTime(now);
    transactionMapper.insert(outTransaction);

    // 创建转入记录（type=收入，category=其他）
    Transaction inTransaction = new Transaction();
    inTransaction.setUserId(userId);
    inTransaction.setAccountId(request.getToAccountId());
    inTransaction.setCategoryId(transferCategoryId);
    inTransaction.setType(TransactionType.INCOME.getValue());
    inTransaction.setAmount(request.getAmount());
    inTransaction.setNote(inNote);
    inTransaction.setTime(now);
    inTransaction.setTransferId(transferId);
    inTransaction.setCreateTime(now);
    inTransaction.setUpdateTime(now);
    transactionMapper.insert(inTransaction);

    // 组装返回结果（复用已加载的 fromAccount/toAccount/transferCategory，消除4次额外DB查询）
    TransferDTO dto = new TransferDTO();
    dto.setTransferId(transferId);
    dto.setOutRecord(toDTO(outTransaction, fromAccount, transferCategory));
    dto.setInRecord(toDTO(inTransaction, toAccount, transferCategory));
    return dto;
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
    entityValidator.validateAccount(userId, accountId);

    // PRD P2-3 异常流程①: 文件大小超过 5MB → 拒绝
    if (file.getSize() > CSV_MAX_FILE_SIZE) {
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE.getCode(), ErrorCode.FILE_TOO_LARGE.getMsg());
    }
    // PRD P2-3 异常流程①: 文件格式非 CSV → 拒绝
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
      throw new BusinessException(ErrorCode.CSV_FORMAT_ONLY.getCode(), ErrorCode.CSV_FORMAT_ONLY.getMsg());
    }

    ImportResultDTO result = new ImportResultDTO();

    // 预加载所有分类到 Map，避免循环内逐行 selectById（种子数据仅13条，1次查询覆盖全部）
    Map<Long, Category> categoryCache = categoryMapper.selectList(new LambdaQueryWrapper<Category>()).stream()
        .collect(Collectors.toMap(Category::getId, c -> c));

    try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String[] line;
      boolean firstLine = true;
      int rowNumber = 1; // 行号从1开始，表头是第1行

      while ((line = reader.readNext()) != null) {
        rowNumber++;
        // 跳过表头
        if (firstLine) {
          firstLine = false;
          continue;
        }

        try {
          // CSV 格式: 日期,分类ID,类型,金额,备注
          if (line.length < 4) {
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();
            failRow.setRow(rowNumber);
            failRow.setReason("列数不足，至少需要4列（日期,分类ID,类型,金额）");
            result.getFailRows().add(failRow);
            continue;
          }

          Transaction transaction = new Transaction();
          transaction.setUserId(userId);
          transaction.setAccountId(accountId);
          long categoryId = Long.parseLong(line[1].trim());
          int type = Integer.parseInt(line[2].trim());
          BigDecimal amount = new BigDecimal(line[3].trim()).setScale(2, RoundingMode.HALF_UP);

          // 语义校验：分类必须存在（从预加载缓存查询，避免逐行DB查询）
          Category category = categoryCache.get(categoryId);
          if (category == null) {
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();
            failRow.setRow(rowNumber);
            failRow.setReason("分类ID " + categoryId + " 不存在");
            result.getFailRows().add(failRow);
            continue;
          }
          // 语义校验：类型必须为 1(收入) 或 2(支出)
          if (type != TransactionType.INCOME.getValue() && type != TransactionType.EXPENSE.getValue()) {
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();
            failRow.setRow(rowNumber);
            failRow.setReason("交易类型必须为1(收入)或2(支出)，当前值: " + type);
            result.getFailRows().add(failRow);
            continue;
          }
          // 语义校验：金额必须大于 0
          if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();
            failRow.setRow(rowNumber);
            failRow.setReason("金额必须大于0，当前值: " + amount);
            result.getFailRows().add(failRow);
            continue;
          }

          transaction.setCategoryId(categoryId);
          transaction.setType(type);
          transaction.setAmount(amount);
          transaction.setNote(line.length > 4 ? line[4].trim() : "");
          transaction.setTime(LocalDateTime.parse(line[0].trim(), DTF));
          transaction.setCreateTime(LocalDateTime.now());
          transaction.setUpdateTime(LocalDateTime.now());

          // PRD P2-3 业务规则①: 单次导入上限 1000 条（超过上限的行记录为失败，不回滚已插入的记录）
          if (result.getSuccessCount() >= CSV_MAX_RECORDS) {
            ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();
            failRow.setRow(rowNumber);
            failRow.setReason("单次导入不能超过 " + CSV_MAX_RECORDS + " 条记录");
            result.getFailRows().add(failRow);
            continue;
          }

          transactionMapper.insert(transaction);
          result.setSuccessCount(result.getSuccessCount() + 1);
        } catch (NumberFormatException e) {
          ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();
          failRow.setRow(rowNumber);
          failRow.setReason("数据格式错误: " + e.getMessage());
          result.getFailRows().add(failRow);
        } catch (Exception e) {
          ImportResultDTO.FailRow failRow = new ImportResultDTO.FailRow();
          failRow.setRow(rowNumber);
          failRow.setReason("解析失败: " + e.getMessage());
          result.getFailRows().add(failRow);
        }
      }
    } catch (Exception e) {
      log.error("CSV 文件读取失败: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.CSV_READ_ERROR.getCode(), ErrorCode.CSV_READ_ERROR.getMsg());
    }

    result.setFailCount(result.getFailRows().size());
    return result;
  }

  /**
   * LIKE 通配符转义：将用户输入中的 %、_、\ 转义为 \%、\_、\\，
   * 防止这些字符被 MySQL LIKE 解释为通配符而非字面字符
   */
  private String escapeLikeKeyword(String keyword) {
    if (keyword == null || keyword.isEmpty()) return keyword;
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  /**
   * 根据ID查询交易记录（校验归属）
   */
  private Transaction getTransactionById(Long userId, Long transactionId) {
    Transaction transaction = transactionMapper.selectById(transactionId);
    if (transaction == null || !transaction.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.RECORD_NOT_FOUND.getCode(), ErrorCode.RECORD_NOT_FOUND.getMsg());
    }
    return transaction;
  }

  /**
   * Entity → DTO 转换（传入预加载的关联对象，避免重复查询）
   */
  private TransactionDTO toDTO(Transaction transaction, Account account, Category category) {
    TransactionDTO dto = new TransactionDTO();
    dto.setId(transaction.getId());
    dto.setAccountId(transaction.getAccountId());
    dto.setCategoryId(transaction.getCategoryId());
    dto.setType(transaction.getType());
    dto.setAmount(transaction.getAmount());
    dto.setNote(transaction.getNote());
    dto.setTime(transaction.getTime() != null ? transaction.getTime().format(DTF) : null);
    dto.setTransferId(transaction.getTransferId());
    dto.setCreateTime(transaction.getCreateTime());
    dto.setUpdateTime(transaction.getUpdateTime());
    if (account != null) dto.setAccountName(account.getName());
    if (category != null) dto.setCategoryName(category.getName());
    return dto;
  }
}
