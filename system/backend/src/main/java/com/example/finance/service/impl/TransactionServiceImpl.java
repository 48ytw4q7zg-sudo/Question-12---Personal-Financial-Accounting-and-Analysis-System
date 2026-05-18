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
   * 查询交易记录（分页 + 筛选）
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
   * 创建交易记录
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
   * 更新交易记录（转账记录仅允许修改备注）
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
   * 转账（@Transactional 保证原子性）
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
   * 导入 CSV（@Transactional 保证批量插入原子性）
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
