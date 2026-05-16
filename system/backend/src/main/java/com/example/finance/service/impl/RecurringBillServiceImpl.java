package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.entity.Account;
import com.example.finance.entity.Category;
import com.example.finance.entity.RecurringBill;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.dto.RecurringBillDTO;
import com.example.finance.entity.dto.RecurringBillRequest;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.mapper.RecurringBillMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.RecurringBillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 周期性账单服务实现
 */
@Service
@RequiredArgsConstructor
public class RecurringBillServiceImpl implements RecurringBillService {

  private static final int STATUS_ACTIVE = 1;
  private static final int STATUS_INACTIVE = 0;

  private final RecurringBillMapper recurringBillMapper;
  private final TransactionMapper transactionMapper;
  private final AccountMapper accountMapper;
  private final CategoryMapper categoryMapper;

  /**
   * 查询用户周期性账单列表（status=1），批量加载关联数据避免 N+1
   */
  @Override
  public List<RecurringBillDTO> list(Long userId) {
    List<RecurringBill> bills = recurringBillMapper.selectList(
        new LambdaQueryWrapper<RecurringBill>()
            .eq(RecurringBill::getUserId, userId)
            .eq(RecurringBill::getStatus, STATUS_ACTIVE)
            .orderByDesc(RecurringBill::getCreateTime)
    );

    // 批量加载关联的账户和分类
    Set<Long> accountIds = bills.stream().map(RecurringBill::getAccountId).collect(Collectors.toSet());
    Set<Long> categoryIds = bills.stream().map(RecurringBill::getCategoryId).collect(Collectors.toSet());

    Map<Long, String> accountNameMap = accountMapper.selectBatchIds(accountIds).stream()
        .collect(Collectors.toMap(Account::getId, Account::getName));
    Map<Long, String> categoryNameMap = categoryMapper.selectBatchIds(categoryIds).stream()
        .collect(Collectors.toMap(Category::getId, Category::getName));

    return bills.stream().map(bill -> toDTO(bill, accountNameMap, categoryNameMap)).toList();
  }

  /**
   * 创建周期性账单
   */
  @Override
  public RecurringBillDTO create(Long userId, RecurringBillRequest request) {
    // 校验账户归属
    validateAccount(userId, request.getAccountId());
    // 校验分类存在
    validateCategory(request.getCategoryId());

    RecurringBill bill = new RecurringBill();
    bill.setUserId(userId);
    bill.setAccountId(request.getAccountId());
    bill.setCategoryId(request.getCategoryId());
    bill.setName(request.getName());
    bill.setAmount(request.getAmount());
    bill.setType(request.getType());
    bill.setPeriod(request.getPeriod());
    bill.setNextDueDate(LocalDate.parse(request.getNextDueDate(), DateTimeFormatter.ISO_LOCAL_DATE));
    bill.setStatus(STATUS_ACTIVE);
    bill.setCreateTime(LocalDateTime.now());
    bill.setUpdateTime(LocalDateTime.now());

    recurringBillMapper.insert(bill);
    return toDTO(bill);
  }

  /**
   * 更新周期性账单
   */
  @Override
  public RecurringBillDTO update(Long userId, Long billId, RecurringBillRequest request) {
    RecurringBill bill = getBillById(userId, billId);

    bill.setName(request.getName());
    bill.setAccountId(request.getAccountId());
    bill.setCategoryId(request.getCategoryId());
    bill.setAmount(request.getAmount());
    bill.setType(request.getType());
    bill.setPeriod(request.getPeriod());
    bill.setNextDueDate(LocalDate.parse(request.getNextDueDate(), DateTimeFormatter.ISO_LOCAL_DATE));
    bill.setUpdateTime(LocalDateTime.now());

    recurringBillMapper.updateById(bill);
    return toDTO(bill);
  }

  /**
   * 停用周期性账单（软删除）
   */
  @Override
  public void deactivate(Long userId, Long billId) {
    RecurringBill bill = getBillById(userId, billId);
    if (bill.getStatus() == STATUS_INACTIVE) {
      throw new BusinessException(5004, "周期性账单已停用");
    }
    bill.setStatus(STATUS_INACTIVE);
    bill.setUpdateTime(LocalDateTime.now());
    recurringBillMapper.updateById(bill);
  }

  /**
   * 生成交易记录（@Transactional 保证原子性）
   */
  @Override
  @Transactional
  public TransactionDTO generate(Long userId, Long billId) {
    RecurringBill bill = getBillById(userId, billId);
    // 校验账单状态
    if (bill.getStatus() == STATUS_INACTIVE) {
      throw new BusinessException(5004, "周期性账单已停用");
    }

    // 创建交易记录
    Transaction transaction = new Transaction();
    transaction.setUserId(userId);
    transaction.setAccountId(bill.getAccountId());
    transaction.setCategoryId(bill.getCategoryId());
    transaction.setType(bill.getType());
    transaction.setAmount(bill.getAmount());
    transaction.setNote("周期性账单生成: " + bill.getName());
    transaction.setTime(LocalDateTime.now());
    transaction.setCreateTime(LocalDateTime.now());
    transaction.setUpdateTime(LocalDateTime.now());

    transactionMapper.insert(transaction);

    // 更新下次到期日
    bill.setNextDueDate(calculateNextDueDate(bill.getNextDueDate(), bill.getPeriod()));
    bill.setUpdateTime(LocalDateTime.now());
    recurringBillMapper.updateById(bill);

    // 转换为 DTO
    TransactionDTO dto = new TransactionDTO();
    dto.setId(transaction.getId());
    dto.setAccountId(transaction.getAccountId());
    dto.setCategoryId(transaction.getCategoryId());
    dto.setType(transaction.getType());
    dto.setAmount(transaction.getAmount());
    dto.setNote(transaction.getNote());
    dto.setTime(transaction.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    dto.setCreateTime(transaction.getCreateTime());
    dto.setUpdateTime(transaction.getUpdateTime());

    // 填充关联名称
    Account account = accountMapper.selectById(bill.getAccountId());
    if (account != null) {
      dto.setAccountName(account.getName());
    }
    Category category = categoryMapper.selectById(bill.getCategoryId());
    if (category != null) {
      dto.setCategoryName(category.getName());
    }

    return dto;
  }

  /**
   * 计算下次到期日
   */
  private LocalDate calculateNextDueDate(LocalDate current, String period) {
    return switch (period) {
      case "daily" -> current.plusDays(1);
      case "weekly" -> current.plusWeeks(1);
      case "monthly" -> current.plusMonths(1);
      case "yearly" -> current.plusYears(1);
      default -> current.plusMonths(1);
    };
  }

  /**
   * 校验账户归属
   */
  private Account validateAccount(Long userId, Long accountId) {
    Account account = accountMapper.selectById(accountId);
    if (account == null || !account.getUserId().equals(userId) || account.getStatus() != 1) {
      throw new BusinessException(2003, "账户不存在");
    }
    return account;
  }

  /**
   * 校验分类存在
   */
  private void validateCategory(Long categoryId) {
    Category category = categoryMapper.selectById(categoryId);
    if (category == null) {
      throw new BusinessException(4001, "分类不存在");
    }
  }

  /**
   * 根据ID查询周期性账单（校验归属）
   */
  private RecurringBill getBillById(Long userId, Long billId) {
    RecurringBill bill = recurringBillMapper.selectById(billId);
    if (bill == null || !bill.getUserId().equals(userId)) {
      throw new BusinessException(5005, "周期性账单不存在");
    }
    return bill;
  }

  /**
   * Entity → DTO 转换（批量查询用，传入预加载的名称映射）
   */
  private RecurringBillDTO toDTO(RecurringBill bill, Map<Long, String> accountNameMap, Map<Long, String> categoryNameMap) {
    RecurringBillDTO dto = new RecurringBillDTO();
    dto.setId(bill.getId());
    dto.setName(bill.getName());
    dto.setAccountId(bill.getAccountId());
    dto.setCategoryId(bill.getCategoryId());
    dto.setAmount(bill.getAmount());
    dto.setType(bill.getType());
    dto.setPeriod(bill.getPeriod());
    dto.setNextDueDate(bill.getNextDueDate() != null ? bill.getNextDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
    dto.setStatus(bill.getStatus());
    dto.setCreateTime(bill.getCreateTime());
    dto.setUpdateTime(bill.getUpdateTime());
    dto.setAccountName(accountNameMap.getOrDefault(bill.getAccountId(), ""));
    dto.setCategoryName(categoryNameMap.getOrDefault(bill.getCategoryId(), ""));
    return dto;
  }

  /**
   * Entity → DTO 转换（单条操作用）
   */
  private RecurringBillDTO toDTO(RecurringBill bill) {
    RecurringBillDTO dto = new RecurringBillDTO();
    dto.setId(bill.getId());
    dto.setName(bill.getName());
    dto.setAccountId(bill.getAccountId());
    dto.setCategoryId(bill.getCategoryId());
    dto.setAmount(bill.getAmount());
    dto.setType(bill.getType());
    dto.setPeriod(bill.getPeriod());
    dto.setNextDueDate(bill.getNextDueDate() != null ? bill.getNextDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
    dto.setStatus(bill.getStatus());
    dto.setCreateTime(bill.getCreateTime());
    dto.setUpdateTime(bill.getUpdateTime());

    Account account = accountMapper.selectById(bill.getAccountId());
    dto.setAccountName(account != null ? account.getName() : "");
    Category category = categoryMapper.selectById(bill.getCategoryId());
    dto.setCategoryName(category != null ? category.getName() : "");

    return dto;
  }
}
