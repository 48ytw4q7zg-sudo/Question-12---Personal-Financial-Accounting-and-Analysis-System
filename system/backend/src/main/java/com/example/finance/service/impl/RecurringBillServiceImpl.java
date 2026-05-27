/**
 * 周期性账单服务实现类（PRD P1-4 周期性账单提醒）
 *
 * 职责：处理周期性账单的创建、查询、更新、停用及到期日自动推进等核心业务逻辑。
 * 调用方：RecurringBillController → RecurringBillService 接口 → 本实现类。
 */
package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.enums.RecurringPeriod;
import com.example.finance.common.enums.Status;
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
import com.example.finance.common.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;  // 日期解析异常（用户输入非法 YYYY-MM-DD 时捕获）
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private static final String RECURRING_NOTE_PREFIX = "周期性账单生成: ";

  /** -> RecurringBillMapper：周期性账单 CRUD 数据访问 */
  private final RecurringBillMapper recurringBillMapper;
  /** -> TransactionMapper：一键生成时插入交易记录 */
  private final TransactionMapper transactionMapper;
  /** -> AccountMapper：校验账户归属 + 批量加载账户名称 */
  private final AccountMapper accountMapper;
  /** -> CategoryMapper：校验分类存在 + 批量加载分类名称 */
  private final CategoryMapper categoryMapper;
  /** -> EntityValidator：跨 Service 共享的 validateAccount/validateCategory 校验（替代本地重复方法） */
  private final EntityValidator entityValidator;

  /**
   * 查询用户周期性账单列表（status=1），批量加载关联数据避免 N+1
   */
  @Override
  @Transactional(readOnly = true)
  public List<RecurringBillDTO> list(Long userId) {
    List<RecurringBill> bills = recurringBillMapper.selectList(  // 查询用户活跃周期性账单列表
        new LambdaQueryWrapper<RecurringBill>()
            .eq(RecurringBill::getUserId, userId)  // 筛选当前用户
            .eq(RecurringBill::getStatus, Status.ACTIVE.getValue())  // 仅查活跃账单
            .orderByDesc(RecurringBill::getCreateTime)  // 按创建时间倒序
    );

    // 批量加载关联的账户和分类（含账户status，用于PRD P1-4异常标记）
    Set<Long> accountIds = bills.stream().map(RecurringBill::getAccountId).filter(Objects::nonNull).collect(Collectors.toSet());  // 收集账户ID集合
    Set<Long> categoryIds = bills.stream().map(RecurringBill::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());  // 收集分类ID集合

    Map<Long, Account> accountMap = accountIds.isEmpty()  // 批量查询关联账户
        ? Map.of()  // 无ID则返回空Map
        : accountMapper.selectByIds(accountIds).stream()  // 一次性查询所有账户
            .collect(Collectors.toMap(Account::getId, a -> a, (a1, a2) -> a1));  // 账户ID→账户对象(重复key取第一个)
    Map<Long, String> accountNameMap = accountMap.entrySet().stream()  // 提取账户名称映射
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getName()));  // 账户ID→名称
    // PRD P1-4 业务规则③: 活跃账单关联账户被禁用 → 标记异常
    Map<Long, Boolean> accountDisabledMap = accountMap.entrySet().stream()  // 提取账户禁用状态映射
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStatus() != null && !Objects.equals(e.getValue().getStatus(), Status.ACTIVE.getValue())));  // 账户ID→是否禁用（Integer用Objects.equals比较值，避免引用比较bug）
    Map<Long, String> categoryNameMap = categoryIds.isEmpty()  // 批量查询分类名称
        ? Map.of()  // 无ID则返回空Map
        : categoryMapper.selectByIds(categoryIds).stream()  // 一次性查询所有分类
            .collect(Collectors.toMap(Category::getId, Category::getName));  // 分类ID→名称

    return bills.stream().map(bill -> toDTO(bill, accountNameMap, accountDisabledMap, categoryNameMap)).toList();  // 遍历转DTO
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
    Account account = entityValidator.validateAccount(userId, request.getAccountId());  // 校验账户归属并返回对象
    // 校验分类存在并复用对象
    Category category = entityValidator.validateCategory(request.getCategoryId());  // 校验分类存在并返回对象
    // 校验下次到期日必须是未来日期（PRD P1-4 异常流程①: 下次到期日为空/过去日期 → 400）
    LocalDate dueDate = parseAndValidateDueDate(request.getNextDueDate());  // 提取公共方法避免重复（create/update共用）
    // 业务校验：金额不能为空且必须大于零
    if (request.getAmount() == null) {  // 金额为null
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "金额不能为空");  // 拒绝null金额
    }
    if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {  // 金额<=0
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "金额必须大于0");  // 拒绝零或负金额
    }

    RecurringBill bill = new RecurringBill();  // 创建账单实体
    bill.setUserId(userId);  // 设置用户ID
    bill.setAccountId(request.getAccountId());  // 设置账户ID
    bill.setCategoryId(request.getCategoryId());  // 设置分类ID
    bill.setName(request.getName());  // 设置账单名称
    bill.setAmount(request.getAmount());  // 设置金额(已通过null和正数校验)
    bill.setType(request.getType());  // 设置类型(1=收入/2=支出)
    bill.setPeriod(request.getPeriod());  // 设置周期(monthly/weekly/daily/yearly)
    bill.setNextDueDate(dueDate);  // 设置下次到期日
    bill.setStatus(Status.ACTIVE.getValue());  // 设置状态为活跃
    bill.setCreateTime(LocalDateTime.now());  // 设置创建时间
    bill.setUpdateTime(LocalDateTime.now());  // 设置更新时间

    recurringBillMapper.insert(bill);  // 插入数据库
    return toDTO(bill, account, category);  // 转为DTO返回
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
    RecurringBill bill = getBillById(userId, billId);  // 查询并校验账单归属

    // 安全校验: 更新时重新校验账户归属当前用户且status=1, 分类存在，复用对象
    Account account = entityValidator.validateAccount(userId, request.getAccountId());  // 校验账户归属
    Category category = entityValidator.validateCategory(request.getCategoryId());  // 校验分类存在
    // 校验下次到期日必须是未来日期（与 create() 一致的异常处理：格式错误返回 400 而非 500）
    LocalDate dueDate = parseAndValidateDueDate(request.getNextDueDate());  // 提取公共方法避免重复（create/update共用）

    bill.setName(request.getName());  // 更新名称
    bill.setAccountId(request.getAccountId());  // 更新账户ID
    bill.setCategoryId(request.getCategoryId());  // 更新分类ID
    bill.setAmount(request.getAmount());  // 更新金额
    bill.setType(request.getType());  // 更新类型
    bill.setPeriod(request.getPeriod());  // 更新周期
    bill.setNextDueDate(dueDate);  // 更新下次到期日
    bill.setUpdateTime(LocalDateTime.now());  // 更新修改时间

    recurringBillMapper.updateById(bill);  // 写入数据库
    return toDTO(bill, account, category);  // 转为DTO返回
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
    RecurringBill bill = getBillById(userId, billId);  // 查询并校验账单归属
    if (Objects.equals(bill.getStatus(), Status.DISABLED.getValue())) {  // 已停用不可再次停用 · Objects.equals 避免 Integer 拆箱 NPE
      throw new BusinessException(ErrorCode.BILL_INACTIVE.getCode(), ErrorCode.BILL_INACTIVE.getMsg());  // 抛出业务异常
    }
    bill.setStatus(Status.DISABLED.getValue());  // 设置状态为停用
    bill.setUpdateTime(LocalDateTime.now());  // 更新修改时间
    recurringBillMapper.updateById(bill);  // 写入数据库
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
    RecurringBill bill = getBillById(userId, billId);  // 查询并校验账单归属
    if (Objects.equals(bill.getStatus(), Status.DISABLED.getValue())) {  // 已停用账单不可生成 · Objects.equals 避免 Integer 拆箱 NPE
      throw new BusinessException(ErrorCode.BILL_INACTIVE.getCode(), ErrorCode.BILL_INACTIVE.getMsg());  // 抛出业务异常
    }

    // PRD P1-4 异常流程②: 一键生成时关联账户已禁用 → 拒绝生成
    // 安全校验: generate() 也需校验 account userId 归属（防数据篡改后账单引用他人账户）
    Account account = accountMapper.selectById(bill.getAccountId());  // 查询关联账户
    if (account == null || !Objects.equals(account.getUserId(), userId) || !Objects.equals(account.getStatus(), Status.ACTIVE.getValue())) {  // 账户不存在/不归属/已禁用 · Objects.equals 避免 Long/Integer 拆箱 NPE
      throw new BusinessException(ErrorCode.BILL_ACCOUNT_DISABLED_GEN.getCode(), ErrorCode.BILL_ACCOUNT_DISABLED_GEN.getMsg());  // 抛出业务异常
    }
    // 安全校验: category 必须存在（分类被删除后账单引用的 categoryId 无效）
    Category category = categoryMapper.selectById(bill.getCategoryId());  // 查询关联分类
    if (category == null) {  // 分类不存在
      throw new BusinessException(ErrorCode.BILL_CATEGORY_NOT_FOUND.getCode(), ErrorCode.BILL_CATEGORY_NOT_FOUND.getMsg());  // 抛出业务异常
    }

    // 创建交易记录
    Transaction transaction = new Transaction();  // 创建交易实体
    transaction.setUserId(userId);  // 设置用户ID
    transaction.setAccountId(bill.getAccountId());  // 设置账户ID
    transaction.setCategoryId(bill.getCategoryId());  // 设置分类ID
    transaction.setType(bill.getType());  // 设置交易类型
    transaction.setAmount(bill.getAmount());  // 设置金额
    transaction.setNote(RECURRING_NOTE_PREFIX + bill.getName());  // 设置备注(标记来源)
    // 安全校验：nextDueDate 可能是 NULL（数据库脏数据或手动插入），需 null 保护
    if (bill.getNextDueDate() == null) {  // nextDueDate为null
      throw new BusinessException(ErrorCode.BILL_DUE_DATE_INVALID.getCode(), "该周期性账单的下次到期日为空，无法生成交易记录");  // 抛出明确的业务异常
    }
    // P1-13 修复(Q-CR Loop2):交易时间策略 — 取「到期日 0:00」与「当前时间」中较小者
    // 旧实现: 永远用 nextDueDate.atStartOfDay() 作为交易时间,
    // 问题: 如用户首次执行 generate 时,到期日已落后多月,生成的交易会跑到过去月份污染历史统计;
    //   反之到期日是未来日(例如 generate 提前触发),交易时间会跑到未来,影响 Dashboard 月度卡片
    // 修复: 用 min(nextDueDate, now),即最早不早于到期日,最晚不晚于现在,落在合理时间窗口
    LocalDateTime nowLdt = LocalDateTime.now();                                       // 当前时间
    LocalDateTime dueLdt = bill.getNextDueDate().atStartOfDay();                      // 到期日零点
    LocalDateTime txTime = dueLdt.isAfter(nowLdt) ? nowLdt : dueLdt;                  // 取较早者:到期日已过用到期日,未到则用当前时间
    transaction.setTime(txTime);  // 设置交易时间(避免落在未来或过早历史)
    transaction.setCreateTime(LocalDateTime.now());  // 设置创建时间
    transaction.setUpdateTime(LocalDateTime.now());  // 设置更新时间

    transactionMapper.insert(transaction);  // 插入交易记录

    // 更新下次到期日 · 循环推进直到未来日期（避免用户忘记生成多月后仍停留在过去日期）
    LocalDate nextDate = calculateNextDueDate(bill.getNextDueDate(), bill.getPeriod());  // 计算下个周期
    int maxIterations = 365;  // 最大推进次数（防极端场景无限循环：如 daily 停用1年后重新激活）
    while (!nextDate.isAfter(LocalDate.now())) {  // 如果下个周期仍是过去日期
      if (--maxIterations <= 0) {  // 超过最大推进次数
        log.warn("周期账单 {} 到期日推进超过最大次数，跳到今天", bill.getId());  // 记录警告
        nextDate = LocalDate.now();  // 强制跳到今天
        break;  // 退出循环
      }
      nextDate = calculateNextDueDate(nextDate, bill.getPeriod());  // 继续推进一个周期
    }
    bill.setNextDueDate(nextDate);  // 设置推进后的到期日
    bill.setUpdateTime(LocalDateTime.now());  // 更新修改时间
    recurringBillMapper.updateById(bill);  // 写入数据库

    // 转换为 DTO（复用已预加载的 account 和 category，消除额外 DB 查询）
    TransactionDTO dto = new TransactionDTO();  // 创建交易DTO
    dto.setId(transaction.getId());  // 设置ID
    dto.setAccountId(transaction.getAccountId());  // 设置账户ID
    dto.setCategoryId(transaction.getCategoryId());  // 设置分类ID
    dto.setType(transaction.getType());  // 设置交易类型
    dto.setAmount(transaction.getAmount());  // 设置金额
    dto.setNote(transaction.getNote());  // 设置备注
    dto.setTime(transaction.getTime().format(DTF));  // 设置格式化时间
    dto.setCreateTime(transaction.getCreateTime());  // 设置创建时间
    dto.setUpdateTime(transaction.getUpdateTime());  // 设置更新时间

    dto.setAccountName(account.getName());  // 设置账户名称
    dto.setCategoryName(category.getName());  // 设置分类名称

    return dto;  // 返回交易DTO
  }

  /**
   * 计算下次到期日（根据周期推进，使用 RecurringPeriod 枚举替代硬编码字符串）
   *
   * <p>周期映射: DAILY=+1天, WEEKLY=+1周, MONTHLY=+1月, YEARLY=+1年。</p>
   * <p>默认兜底: 未知周期按MONTHLY处理（RecurringPeriod.fromValue 兜底）。</p>
   *
   * @param current 当前到期日
   * @param period 周期(monthly/weekly/daily/yearly)
   * @return 新的到期日
   */
  private LocalDate calculateNextDueDate(LocalDate current, String period) {  // 计算下次到期日
    RecurringPeriod rp = RecurringPeriod.fromValue(period);  // 将字符串转为枚举
    return switch (rp) {  // 根据周期类型推进
      case DAILY -> current.plusDays(1);  // 日周期+1天
      case WEEKLY -> current.plusWeeks(1);  // 周周期+1周
      case MONTHLY -> current.plusMonths(1);  // 月周期+1月
      case YEARLY -> current.plusYears(1);  // 年周期+1年
    };
  }

  /**
   * 解析并校验到期日期（提取公共方法避免 create/update 重复）
   *
   * <p>校验规则：</p>
   * <ol>
   *   <li>日期格式必须为 YYYY-MM-DD（ISO_LOCAL_DATE）</li>
   *   <li>到期日必须是未来日期（isAfter(LocalDate.now())）</li>
   * </ol>
   *
   * @param nextDueDate 到期日字符串（YYYY-MM-DD 格式）
   * @return 解析后的 LocalDate
   * @throws BusinessException 3013=日期格式错误 / 5004=到期日必须是未来日期
   */
  private LocalDate parseAndValidateDueDate(String nextDueDate) {  // 解析并校验到期日期
    LocalDate dueDate;  // 到期日变量
    try {
      dueDate = LocalDate.parse(nextDueDate, DateTimeFormatter.ISO_LOCAL_DATE);  // 解析日期字符串
    } catch (DateTimeParseException e) {  // 捕获日期解析异常
      throw new BusinessException(ErrorCode.DATE_FORMAT_INVALID.getCode(), ErrorCode.DATE_FORMAT_INVALID.getMsg());  // 抛出业务异常
    }
    if (!dueDate.isAfter(LocalDate.now())) {  // 到期日不是未来日期
      throw new BusinessException(ErrorCode.BILL_DUE_DATE_INVALID.getCode(), ErrorCode.BILL_DUE_DATE_INVALID.getMsg());  // 抛出业务异常
    }
    return dueDate;  // 返回解析后的到期日
  }

  /**
   * 根据ID查询周期性账单（校验归属）
   */
  private RecurringBill getBillById(Long userId, Long billId) {  // 查询账单并校验归属
    RecurringBill bill = recurringBillMapper.selectById(billId);  // 根据ID查询账单
    // 校验：账单存在 + 归属当前用户（Integer用Objects.equals比较值，避免引用比较bug和null userId NPE）
    if (bill == null || !Objects.equals(bill.getUserId(), userId)) {
      throw new BusinessException(ErrorCode.BILL_NOT_FOUND.getCode(), ErrorCode.BILL_NOT_FOUND.getMsg());  // 抛出业务异常
    }
    return bill;  // 返回账单实体
  }

  /**
   * Entity → DTO 转换（批量查询用，传入预加载的名称映射）
   * <p>调用方: list() 方法（service/impl/RecurringBillServiceImpl.java 第105行）</p>
   */
  private RecurringBillDTO toDTO(RecurringBill bill, Map<Long, String> accountNameMap, Map<Long, Boolean> accountDisabledMap, Map<Long, String> categoryNameMap) {  // Entity→DTO批量版
    RecurringBillDTO dto = new RecurringBillDTO();  // 创建DTO对象
    fillBaseFields(dto, bill);  // 填充基础字段（消除两个toDTO方法的重复代码）
    dto.setAccountName(accountNameMap.getOrDefault(bill.getAccountId(), ""));  // 设置账户名称(从预加载映射获取)
    dto.setCategoryName(categoryNameMap.getOrDefault(bill.getCategoryId(), ""));  // 设置分类名称(从预加载映射获取)
    // PRD P1-4 业务规则③: 活跃账单关联账户被禁用 → accountDisabled=true
    dto.setAccountDisabled(accountDisabledMap.getOrDefault(bill.getAccountId(), false));  // 设置账户是否禁用
    return dto;  // 返回DTO
  }

  /**
   * Entity → DTO 转换（传入预加载的关联对象，避免重复查询）
   * <p>调用方: create() / update() / generate() 方法（service/impl/RecurringBillServiceImpl.java）</p>
   */
  private RecurringBillDTO toDTO(RecurringBill bill, Account account, Category category) {  // Entity→DTO对象版
    RecurringBillDTO dto = new RecurringBillDTO();  // 创建DTO对象
    fillBaseFields(dto, bill);  // 填充基础字段（消除两个toDTO方法的重复代码）
    dto.setAccountName(account != null ? account.getName() : "");  // 设置账户名称(null保护)
    dto.setAccountDisabled(account != null && !Objects.equals(account.getStatus(), Status.ACTIVE.getValue()));  // 设置账户是否禁用（Integer用Objects.equals比较值，避免引用比较bug）
    dto.setCategoryName(category != null ? category.getName() : "");  // 设置分类名称(null保护)
    return dto;  // 返回DTO
  }

  /**
   * 填充 RecurringBillDTO 基础字段（提取公共逻辑，消除两个 toDTO 方法的代码重复）
   * <p>被两个 toDTO 重载方法共用，避免字段映射逻辑重复维护。</p>
   *
   * @param dto  待填充的DTO对象
   * @param bill 周期性账单实体
   */
  private void fillBaseFields(RecurringBillDTO dto, RecurringBill bill) {  // 填充DTO基础字段
    dto.setId(bill.getId());  // 设置ID
    dto.setName(bill.getName());  // 设置名称
    dto.setAccountId(bill.getAccountId());  // 设置账户ID
    dto.setCategoryId(bill.getCategoryId());  // 设置分类ID
    dto.setAmount(bill.getAmount());  // 设置金额
    dto.setType(bill.getType());  // 设置类型
    dto.setPeriod(bill.getPeriod());  // 设置周期
    dto.setNextDueDate(bill.getNextDueDate() != null ? bill.getNextDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null);  // 设置到期日(null保护)
    dto.setStatus(bill.getStatus());  // 设置状态
    dto.setCreateTime(bill.getCreateTime());  // 设置创建时间
    dto.setUpdateTime(bill.getUpdateTime());  // 设置更新时间
  }
}
