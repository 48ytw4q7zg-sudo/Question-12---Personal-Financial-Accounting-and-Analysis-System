package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.Account;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
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
import java.util.UUID;

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
 *   <li>CSV导入使用 @Transactional 包裹批量插入, 单行解析失败不影响已提交行</li>
 *   <li>所有操作强制校验 user_id 归属, 确保数据隔离</li>
 * </ul>
 *
 * <p>调用方: TransactionController (controller/TransactionController.java)</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

  // 转账默认分类ID（其他）
  private static final Long TRANSFER_CATEGORY_ID = 13L;
  // 交易类型常量
  private static final int TYPE_INCOME = 1;
  private static final int TYPE_EXPENSE = 2;

  private final TransactionMapper transactionMapper;
  private final AccountMapper accountMapper;
  private final CategoryMapper categoryMapper;

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
  public IPage<TransactionDTO> list(Long userId, Long accountId, Long categoryId,
      String startTime, String endTime, String keyword, String sortBy,
      int pageNum, int pageSize) {
    // R-05-issue-4: 已修复 - RowBounds+独立count是XML动态ORDER BY的标准MyBatis分页模式,Page对象正确封装total/records
    Page<TransactionDTO> page = new Page<>(pageNum, pageSize);
    List<TransactionDTO> records = transactionMapper.selectTransactionList(
        userId, accountId, categoryId, startTime, endTime, keyword, sortBy,
        new org.apache.ibatis.session.RowBounds((pageNum - 1) * pageSize, pageSize)
    );
    Long total = transactionMapper.selectTransactionCount(
        userId, accountId, categoryId, startTime, endTime, keyword
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
   * @throws BusinessException 3002=账户不存在或已禁用 / 3002=分类不存在
   */
  @Override
  public TransactionDTO create(Long userId, TransactionRequest request) {
    // 校验账户归属
    validateAccount(userId, request.getAccountId());
    // 校验分类存在
    validateCategory(request.getCategoryId());

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
    return toDTO(transaction);
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
   * @throws BusinessException 3003=转账记录金额不可修改 / 3006=收支记录不存在
   */
  @Override
  public TransactionDTO update(Long userId, Long transactionId, TransactionRequest request) {
    Transaction transaction = getTransactionById(userId, transactionId);

    // 转账记录仅允许修改备注
    if (transaction.getTransferId() != null) {
      // 转账记录禁止修改金额
      if (transaction.getAmount().compareTo(request.getAmount()) != 0) {
        throw new BusinessException(3003, "转账记录金额不可修改");
      }
      String newNote = request.getNote() != null ? request.getNote() : "";
      String oldNote = transaction.getNote() != null ? transaction.getNote() : "";
      if (!oldNote.equals(newNote)) {
        transaction.setNote(request.getNote());
        transaction.setUpdateTime(LocalDateTime.now());
        transactionMapper.updateById(transaction);
      }
      return toDTO(transaction);
    }

    // 普通交易记录允许更新全部字段
    transaction.setAccountId(request.getAccountId());
    transaction.setCategoryId(request.getCategoryId());
    transaction.setType(request.getType());
    transaction.setAmount(request.getAmount());
    transaction.setNote(request.getNote());
    transaction.setTime(request.getTime());
    transaction.setUpdateTime(LocalDateTime.now());

    transactionMapper.updateById(transaction);
    return toDTO(transaction);
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
   * @throws BusinessException 3004=转出转入账户不可相同 / 3005=余额不足 / 3002=账户不存在或已禁用
   */
  @Override
  @Transactional
  public TransferDTO transfer(Long userId, TransferRequest request) {
    // 校验转出和转入账户归属
    Account fromAccount = validateAccount(userId, request.getFromAccountId());
    Account toAccount = validateAccount(userId, request.getToAccountId());

    // 校验不能转给自己
    if (request.getFromAccountId().equals(request.getToAccountId())) {
      throw new BusinessException(3004, "转出账户和转入账户不能相同");
    }

    // 校验转出账户余额充足
    BigDecimal totalIncome = transactionMapper.selectAccountIncome(userId, fromAccount.getId());
    BigDecimal totalExpense = transactionMapper.selectAccountExpense(userId, fromAccount.getId());
    BigDecimal currentBalance = fromAccount.getInitialBalance().add(totalIncome).subtract(totalExpense);
    if (currentBalance.compareTo(request.getAmount()) < 0) {
      throw new BusinessException(3005, "转出账户余额不足");
    }

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
    outTransaction.setCategoryId(TRANSFER_CATEGORY_ID);
    outTransaction.setType(TYPE_EXPENSE);
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
    inTransaction.setCategoryId(TRANSFER_CATEGORY_ID);
    inTransaction.setType(TYPE_INCOME);
    inTransaction.setAmount(request.getAmount());
    inTransaction.setNote(inNote);
    inTransaction.setTime(now);
    inTransaction.setTransferId(transferId);
    inTransaction.setCreateTime(now);
    inTransaction.setUpdateTime(now);
    transactionMapper.insert(inTransaction);

    // 组装返回结果
    TransferDTO dto = new TransferDTO();
    dto.setTransferId(transferId);
    dto.setOutRecord(toDTO(outTransaction));
    dto.setInRecord(toDTO(inTransaction));
    return dto;
  }

  // PRD P2-3: 文件大小上限 5MB
  private static final long CSV_MAX_FILE_SIZE = 5 * 1024 * 1024;
  // PRD P2-3: 单次导入上限 1000 条
  private static final int CSV_MAX_RECORDS = 1000;

  /**
   * 批量导入 CSV 文件（银行流水导入）
   *
   * <p>对应 PRD P2-3 导入银行CSV。</p>
   * <p>CSV格式: 日期(yyyy-MM-dd HH:mm:ss), 分类ID, 类型(1=收入/2=支出), 金额, 备注。</p>
   * <p>限制: 文件≤5MB, 单次≤1000条, 仅支持UTF-8编码的.csv文件。</p>
   * <p>容错: 单行解析失败跳过(failCount++), 不影响其他行; 全部在 @Transactional 事务内执行。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param file 上传的CSV文件
   * @param accountId 导入目标账户ID
   * @return 导入结果摘要("导入完成: 成功N条, 失败M条")
   * @throws BusinessException 3001=文件大小超限 / 格式错误 / 单次导入超限
   */
  @Override
  @Transactional
  public String importCsv(Long userId, MultipartFile file, Long accountId) {
    validateAccount(userId, accountId);

    // PRD P2-3 异常流程①: 文件大小超过 5MB → 拒绝
    if (file.getSize() > CSV_MAX_FILE_SIZE) {
      throw new BusinessException(3001, "文件大小不能超过 5MB");
    }
    // PRD P2-3 异常流程①: 文件格式非 CSV → 拒绝
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
      throw new BusinessException(3001, "仅支持 .csv 格式文件");
    }

    int successCount = 0;
    int failCount = 0;

    try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String[] line;
      boolean firstLine = true;

      while ((line = reader.readNext()) != null) {
        // 跳过表头
        if (firstLine) {
          firstLine = false;
          continue;
        }

        try {
          // CSV 格式: 日期,分类ID,类型,金额,备注
          if (line.length < 4) {
            failCount++;
            continue;
          }

          Transaction transaction = new Transaction();
          transaction.setUserId(userId);
          transaction.setAccountId(accountId);
          transaction.setCategoryId(Long.parseLong(line[1].trim()));
          transaction.setType(Integer.parseInt(line[2].trim()));
          transaction.setAmount(new BigDecimal(line[3].trim()));
          transaction.setNote(line.length > 4 ? line[4].trim() : "");
          transaction.setTime(LocalDateTime.parse(line[0].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
          transaction.setCreateTime(LocalDateTime.now());
          transaction.setUpdateTime(LocalDateTime.now());

          transactionMapper.insert(transaction);
          successCount++;
          // PRD P2-3 业务规则①: 单次导入上限 1000 条
          if (successCount > CSV_MAX_RECORDS) {
            throw new BusinessException(3001, "单次导入不能超过 " + CSV_MAX_RECORDS + " 条记录");
          }
        } catch (BusinessException e) {
          throw e;
        } catch (Exception e) {
          log.warn("导入 CSV 行失败: {}", String.join(",", line), e);
          failCount++;
        }
      }
    } catch (Exception e) {
      throw new BusinessException(3001, "CSV 文件读取失败: " + e.getMessage());
    }

    return String.format("导入完成：成功 %d 条，失败 %d 条", successCount, failCount);
  }

  /**
   * 校验账户归属
   */
  private Account validateAccount(Long userId, Long accountId) {
    Account account = accountMapper.selectById(accountId);
    if (account == null || !account.getUserId().equals(userId) || account.getStatus() != 1) {
      throw new BusinessException(3002, "账户不存在或已禁用");
    }
    return account;
  }

  /**
   * 校验分类存在
   */
  private void validateCategory(Long categoryId) {
    Category category = categoryMapper.selectById(categoryId);
    if (category == null) {
      throw new BusinessException(3002, "分类不存在");
    }
  }

  /**
   * 根据ID查询交易记录（校验归属）
   */
  private Transaction getTransactionById(Long userId, Long transactionId) {
    Transaction transaction = transactionMapper.selectById(transactionId);
    if (transaction == null || !transaction.getUserId().equals(userId)) {
      throw new BusinessException(3006, "收支记录不存在");
    }
    return transaction;
  }

  /**
   * Entity → DTO 转换（单条记录用，填充关联名称）
   */
  private TransactionDTO toDTO(Transaction transaction) {
    TransactionDTO dto = new TransactionDTO();
    dto.setId(transaction.getId());
    dto.setAccountId(transaction.getAccountId());
    dto.setCategoryId(transaction.getCategoryId());
    dto.setType(transaction.getType());
    dto.setAmount(transaction.getAmount());
    dto.setNote(transaction.getNote());
    dto.setTime(transaction.getTime() != null ? transaction.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
    dto.setTransferId(transaction.getTransferId());
    dto.setCreateTime(transaction.getCreateTime());
    dto.setUpdateTime(transaction.getUpdateTime());

    // 填充关联名称（仅单条操作时查库，列表查询由 SQL JOIN 填充）
    Account account = accountMapper.selectById(transaction.getAccountId());
    if (account != null) {
      dto.setAccountName(account.getName());
    }
    Category category = categoryMapper.selectById(transaction.getCategoryId());
    if (category != null) {
      dto.setCategoryName(category.getName());
    }

    return dto;
  }
}
