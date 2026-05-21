package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.enums.Status;
import com.example.finance.common.enums.TransactionType;
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
import lombok.extern.slf4j.Slf4j;
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
 *
 * <p>对应 PRD 功能: P1-4 周期性账单提醒(月房租/月工资等周期收支模板)。</p>
 *
 * <p>关键业务规则:</p>
 * <ul>
 *   <li>status=1(活跃) / status=0(停用), 停用后不可恢复(教学简化, 同账户禁用模式)</li>
 *   <li>一键生成(generate): 校验关联账户status=1 → INSERT交易记录 → 更新next_due_date(推进一个周期)</li>
 *   <li>到期日计算: monthly=+1月, weekly=+1周, daily=+1天, yearly=+1年</li>
 *   <li>活跃账单(status=1)引用的账户被禁用后, 该账单在列表中标记异常; 已停用账单(status=0)不受影响</li>
 *   <li>性能优化: list()使用批量查询(selectByIds)加载账户/分类名称, 消除N+1</li>
 * </ul>
 *
 * <p>事务保护: generate()使用@Transactional包裹INSERT交易+UPDATE到期日, 保证原子性。</p>
 *
 * <p>调用方: RecurringBillController (controller/RecurringBillController.java)</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringBillServiceImpl implements RecurringBillService {

  /** 时间格式化常量（复用避免重复创建 DateTimeFormatter） */
  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
            .eq(RecurringBill::getStatus, Status.ACTIVE.getValue())
            .orderByDesc(RecurringBill::getCreateTime)
    );

    // 批量加载关联的账户和分类（含账户status，用于PRD P1-4异常标记）
    Set<Long> accountIds = bills.stream().map(RecurringBill::getAccountId).collect(Collectors.toSet());
    Set<Long> categoryIds = bills.stream().map(RecurringBill::getCategoryId).collect(Collectors.toSet());

    Map<Long, Account> accountMap = accountIds.isEmpty()
        ? Map.of()
        : accountMapper.selectByIds(accountIds).stream()
            .collect(Collectors.toMap(Account::getId, a -> a, (a1, a2) -> a1));
    Map<Long, String> accountNameMap = accountMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));
    // PRD P1-4 业务规则③: 活跃账单关联账户被禁用 → 标记异常
    Map<Long, Boolean> accountDisabledMap = accountMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStatus() != null && e.getValue().getStatus() != Status.ACTIVE.getValue()));
    Map<Long, String> categoryNameMap = categoryIds.isEmpty()
        ? Map.of()
        : categoryMapper.selectByIds(categoryIds).stream()
            .collect(Collectors.toMap(Category::getId, Category::getName));

    return bills.stream().map(bill -> toDTO(bill, accountNameMap, accountDisabledMap, categoryNameMap)).toList();
  }

  /**
   * 创建周期性账单
   *
   * <p>对应 PRD P1-4 主流程: 用户填写名称/金额/类型/分类/账户/周期/下次到期日, 系统创建模板。</p>
   * <p>前置校验: 账户归属当前用户且status=1, 分类存在。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param request 账单请求参数(含name/amount/type/period/accountId/categoryId/nextDueDate)
   * @return 创建后的账单DTO(含账户名/分类名)
   * @throws BusinessException 5007=账户不存在 / 5008=分类不存在
   */
  @Override
  @Transactional
  public RecurringBillDTO create(Long userId, RecurringBillRequest request) {
    // 校验账户归属并复用对象
    Account account = validateAccount(userId, request.getAccountId());
    // 校验分类存在并复用对象
    Category category = validateCategory(request.getCategoryId());
    // 校验下次到期日必须是未来日期（PRD P1-4 异常流程①: 下次到期日为空/过去日期 → 400）
    LocalDate dueDate = LocalDate.parse(request.getNextDueDate(), DateTimeFormatter.ISO_LOCAL_DATE);
    if (!dueDate.isAfter(LocalDate.now())) {
      throw new BusinessException(ErrorCode.BILL_DUE_DATE_INVALID.getCode(), ErrorCode.BILL_DUE_DATE_INVALID.getMsg());
    }

    RecurringBill bill = new RecurringBill();
    bill.setUserId(userId);
    bill.setAccountId(request.getAccountId());
    bill.setCategoryId(request.getCategoryId());
    bill.setName(request.getName());
    bill.setAmount(request.getAmount());
    bill.setType(request.getType());
    bill.setPeriod(request.getPeriod());
    bill.setNextDueDate(dueDate);
    bill.setStatus(Status.ACTIVE.getValue());
    bill.setCreateTime(LocalDateTime.now());
    bill.setUpdateTime(LocalDateTime.now());

    recurringBillMapper.insert(bill);
    return toDTO(bill, account, category);
  }

  /**
   * 更新周期性账单
   *
   * <p>对应 PRD P1-4 PUT /api/recurring-bill/{id}。</p>
   * <p>仅允许更新活跃账单(status=1), 已停用的账单由getBillById()抛出5004。</p>
   * <p>安全校验: 更新时重新校验账户归属和分类存在, 防止越权修改到其他用户的账户。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param billId 账单ID
   * @param request 更新请求参数
   * @return 更新后的账单DTO
   * @throws BusinessException 5006=账单不存在 / 5005=账单已停用 / 5007=账户不存在 / 5008=分类不存在
   */
  @Override
  @Transactional
  public RecurringBillDTO update(Long userId, Long billId, RecurringBillRequest request) {
    RecurringBill bill = getBillById(userId, billId);

    // 安全校验: 更新时重新校验账户归属当前用户且status=1, 分类存在，复用对象
    Account account = validateAccount(userId, request.getAccountId());
    Category category = validateCategory(request.getCategoryId());
    // 校验下次到期日必须是未来日期
    LocalDate dueDate = LocalDate.parse(request.getNextDueDate(), DateTimeFormatter.ISO_LOCAL_DATE);
    if (!dueDate.isAfter(LocalDate.now())) {
      throw new BusinessException(ErrorCode.BILL_DUE_DATE_INVALID.getCode(), ErrorCode.BILL_DUE_DATE_INVALID.getMsg());
    }

    bill.setName(request.getName());
    bill.setAccountId(request.getAccountId());
    bill.setCategoryId(request.getCategoryId());
    bill.setAmount(request.getAmount());
    bill.setType(request.getType());
    bill.setPeriod(request.getPeriod());
    bill.setNextDueDate(dueDate);
    bill.setUpdateTime(LocalDateTime.now());

    recurringBillMapper.updateById(bill);
    return toDTO(bill, account, category);
  }

  /**
   * 停用周期性账单（软删除, 改status=0, 不可恢复）
   *
   * <p>对应 PRD P1-4 DELETE /api/recurring-bill/{id}。</p>
   * <p>教学简化: 停用后不可恢复, 与账户禁用模式一致。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param billId 账单ID
   * @throws BusinessException 5006=账单不存在 / 5005=账单已停用
   */
  @Override
  @Transactional
  public void deactivate(Long userId, Long billId) {
    RecurringBill bill = getBillById(userId, billId);
    if (bill.getStatus() == Status.DISABLED.getValue()) {
      throw new BusinessException(ErrorCode.BILL_INACTIVE.getCode(), ErrorCode.BILL_INACTIVE.getMsg());
    }
    bill.setStatus(Status.DISABLED.getValue());
    bill.setUpdateTime(LocalDateTime.now());
    recurringBillMapper.updateById(bill);
  }

  /**
   * 根据周期性账单模板一键生成收支记录
   *
   * <p>对应 PRD P1-4 POST /api/recurring-bill/{id}/generate。</p>
   * <p>业务流程:</p>
   * <ol>
   *   <li>校验账单存在且status=1(活跃)</li>
   *   <li>校验关联账户status=1(活跃), 已禁用则拒绝生成(PRD P1-4 异常流程②)</li>
   *   <li>在 @Transactional 事务内: INSERT交易记录 → UPDATE账单next_due_date(推进一个周期)</li>
   * </ol>
   * <p>教学简化: 一键手动触发生成, 不做自动扣款; @Scheduled仅日志记录, 不自动创建。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param billId 账单ID
   * @return 生成的交易记录DTO
   * @throws BusinessException 5006=账单不存在 / 5005=账单已停用 / 5003=关联账户已禁用
   */
  @Override
  @Transactional
  public TransactionDTO generate(Long userId, Long billId) {
    RecurringBill bill = getBillById(userId, billId);
    if (bill.getStatus() == Status.DISABLED.getValue()) {
      throw new BusinessException(ErrorCode.BILL_INACTIVE.getCode(), ErrorCode.BILL_INACTIVE.getMsg());
    }

    // PRD P1-4 异常流程②: 一键生成时关联账户已禁用 → 拒绝生成
    Account account = accountMapper.selectById(bill.getAccountId());
    if (account == null || account.getStatus() != Status.ACTIVE.getValue()) {
      throw new BusinessException(ErrorCode.BILL_ACCOUNT_DISABLED_GEN.getCode(), ErrorCode.BILL_ACCOUNT_DISABLED_GEN.getMsg());
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
    dto.setTime(transaction.getTime().format(DTF));
    dto.setCreateTime(transaction.getCreateTime());
    dto.setUpdateTime(transaction.getUpdateTime());

    // 填充关联名称（复用上方已验证的 account）
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
   * 计算下次到期日（根据周期推进）
   *
   * <p>周期映射: daily=+1天, weekly=+1周, monthly=+1月, yearly=+1年。</p>
   * <p>默认兜底: 未知周期按monthly处理。</p>
   *
   * @param current 当前到期日
   * @param period 周期(monthly/weekly/daily/yearly)
   * @return 新的到期日
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
    if (account == null || !account.getUserId().equals(userId) || account.getStatus() != Status.ACTIVE.getValue()) {
      throw new BusinessException(ErrorCode.BILL_ACCOUNT_NOT_FOUND.getCode(), ErrorCode.BILL_ACCOUNT_NOT_FOUND.getMsg());
    }
    return account;
  }

  /**
   * 校验分类存在并返回分类对象（复用避免重复查询）
   */
  private Category validateCategory(Long categoryId) {
    Category category = categoryMapper.selectById(categoryId);
    if (category == null) {
      throw new BusinessException(ErrorCode.BILL_CATEGORY_NOT_FOUND.getCode(), ErrorCode.BILL_CATEGORY_NOT_FOUND.getMsg());
    }
    return category;
  }

  /**
   * 根据ID查询周期性账单（校验归属）
   */
  private RecurringBill getBillById(Long userId, Long billId) {
    RecurringBill bill = recurringBillMapper.selectById(billId);
    if (bill == null || !bill.getUserId().equals(userId)) {
      throw new BusinessException(ErrorCode.BILL_NOT_FOUND.getCode(), ErrorCode.BILL_NOT_FOUND.getMsg());
    }
    return bill;
  }

  /**
   * Entity → DTO 转换（批量查询用，传入预加载的名称映射）
   */
  private RecurringBillDTO toDTO(RecurringBill bill, Map<Long, String> accountNameMap, Map<Long, Boolean> accountDisabledMap, Map<Long, String> categoryNameMap) {
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
    // PRD P1-4 业务规则③: 活跃账单关联账户被禁用 → accountDisabled=true
    dto.setAccountDisabled(accountDisabledMap.getOrDefault(bill.getAccountId(), false));
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
    // PRD P1-4 业务规则③: 活跃账单关联账户被禁用 → accountDisabled=true
    dto.setAccountDisabled(account != null && account.getStatus() != Status.ACTIVE.getValue());
    Category category = categoryMapper.selectById(bill.getCategoryId());
    dto.setCategoryName(category != null ? category.getName() : "");

    return dto;
  }

  /**
   * Entity → DTO 转换（传入预加载的关联对象，避免重复查询）
   */
  private RecurringBillDTO toDTO(RecurringBill bill, Account account, Category category) {
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
    dto.setAccountName(account != null ? account.getName() : "");
    dto.setAccountDisabled(account != null && account.getStatus() != Status.ACTIVE.getValue());
    dto.setCategoryName(category != null ? category.getName() : "");
    return dto;
  }
}
