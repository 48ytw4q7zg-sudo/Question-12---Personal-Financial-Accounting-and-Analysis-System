package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.EntityValidator;
import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.enums.CategoryType;
import com.example.finance.common.enums.TransactionType;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.dto.BudgetDTO;
import com.example.finance.entity.dto.BudgetProgressDTO;
import com.example.finance.entity.dto.BudgetRequest;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import com.example.finance.entity.dto.CategorySummaryDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预算服务实现
 *
 * <p>对应 PRD 功能: P1-3 预算管理(月预算按分类设置 + 超支标记)。</p>
 *
 * <p>关键业务规则:</p>
 * <ul>
 *   <li>预算仅适用于支出分类(category.type=1), 收入分类不可设置预算 (PRD P1-3 异常流程②)</li>
 *   <li>同一用户+同一分类+同一月份仅一条预算, 重复保存时覆盖写入(先查后改, 并发时DuplicateKeyException兜底)</li>
 *   <li>预算进度 = 本月该分类实际支出 / 预算金额 × 100%, 超支标记(overspent=true)当已用>预算</li>
 *   <li>getProgress() 使用批量查询消除N+1: 先查全部预算, 再一次性查各分类支出汇总, 最后内存聚合</li>
 *   <li>预警(getAlert)仅返回超支项(overspent=true), 由前端BudgetPage展示红色警告</li>
 * </ul>
 *
 * <p>并发安全: save() 使用 @Transactional + DuplicateKeyException 捕获并发插入冲突, 回退为更新。</p>
 *
 * <p>调用方: BudgetController (controller/BudgetController.java)</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

  /** 百分比乘数（spent/budget × 100） */
  private static final BigDecimal PERCENTAGE_FACTOR = BigDecimal.valueOf(100);

  /** → BudgetMapper：预算表 CRUD */
  private final BudgetMapper budgetMapper;
  /** → CategoryMapper：分类表查询（校验支出分类 + 加载分类名称） */
  private final CategoryMapper categoryMapper;
  /** → TransactionMapper：交易记录聚合查询（本月分类支出汇总） */
  private final TransactionMapper transactionMapper;

  /**
   * 查询用户指定月份的预算列表
   *
   * <p>对应 PRD P1-3 GET /api/budget。</p>
   * <p>批量加载分类名称(避免N+1查询), 无预算的支出分类不返回。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如"2026"), 空时默认当前年
   * @param month 月份(如"5"), 空时默认当前月
   * @return 该月所有预算列表(含分类名称)
   */
  @Override
  @Transactional(readOnly = true)
  public List<BudgetDTO> list(Long userId, String year, String month) {
    String monthStr = EntityValidator.defaultAndFormatYearMonth(year, month);  // 格式化年月(空值默认当前)
    // 使用 EntityValidator 安全解析方法替代脆弱的 substring+indexOf
    year = String.valueOf(EntityValidator.extractYear(monthStr));  // 从格式化字符串提取年份
    month = String.valueOf(EntityValidator.extractMonth(monthStr));  // 从格式化字符串提取月份

    List<Budget> budgets = budgetMapper.selectList(  // 查询用户该月预算列表
        new LambdaQueryWrapper<Budget>()
            .eq(Budget::getUserId, userId)  // 筛选当前用户
            .eq(Budget::getMonth, monthStr)  // 筛选指定月份
    );

    // 批量加载分类名称，避免 N+1 查询
    Set<Long> categoryIds = budgets.stream().map(Budget::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());  // 收集分类ID集合
    Map<Long, String> categoryNameMap = categoryIds.isEmpty()  // 批量查询分类名称
        ? Map.of()  // 无分类ID则返回空Map
        : categoryMapper.selectByIds(categoryIds).stream()  // 一次性查询所有分类
            .collect(Collectors.toMap(Category::getId, Category::getName));  // 分类ID→名称映射

    return budgets.stream().map(b -> {  // 遍历预算列表转DTO
      BudgetDTO dto = new BudgetDTO();  // 创建DTO对象
      dto.setId(b.getId());  // 设置预算ID
      dto.setCategoryId(b.getCategoryId());  // 设置分类ID
      dto.setMonth(b.getMonth());  // 设置月份
      dto.setAmount(b.getAmount());  // 设置预算金额
      dto.setCreateTime(b.getCreateTime());  // 设置创建时间
      dto.setUpdateTime(b.getUpdateTime());  // 设置更新时间
      dto.setCategoryName(categoryNameMap.getOrDefault(b.getCategoryId(), ""));  // 设置分类名称(空则默认空字符串)
      return dto;  // 返回DTO
    }).toList();  // 转为不可变列表
  }

  /**
   * 保存预算（有则更新, 无则插入 · @Transactional 保证并发安全）
   *
   * <p>对应 PRD P1-3 POST /api/budget。</p>
   * <p>幂等策略: 先查询是否存在同用户+同分类+同月预算, 存在则updateById, 不存在则insert。</p>
   * <p>并发安全: insert时若被唯一索引uk_budget_user_category_month拦截(DuplicateKeyException),
   * 重新查询已存在记录并转为updateById, 确保并发场景下不报错。</p>
   * <p>校验: 分类必须存在且为支出分类(type=1)。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param request 预算请求参数(含categoryId/month/amount)
   * @return 保存后的预算DTO(含分类名称)
   * @throws BusinessException 4003=分类不存在 / 4002=预算仅可设置在支出分类上
   */
  @Override
  @Transactional
  public BudgetDTO save(Long userId, BudgetRequest request) {
    // 校验分类存在
    Category category = categoryMapper.selectById(request.getCategoryId());  // 根据ID查询分类
    if (category == null) {  // 分类不存在
      throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND_FOR_BUDGET.getCode(), ErrorCode.CATEGORY_NOT_FOUND_FOR_BUDGET.getMsg());  // 抛出业务异常
    }
    // 预算仅针对支出分类（category.type=1 为支出）· Objects.equals 避免 Integer 自动拆箱 NPE
    if (!Objects.equals(category.getType(), CategoryType.EXPENSE.getValue())) {  // 非支出分类
      throw new BusinessException(ErrorCode.BUDGET_EXPENSE_ONLY.getCode(), ErrorCode.BUDGET_EXPENSE_ONLY.getMsg());  // 抛出业务异常
    }

    // P1-6 修复(Q-CR Loop1):月份格式严格校验,使用 java.time.YearMonth 解析
    // 旧实现仅用正则 ^\d{4}-\d{2}$,会放过非法月份(2026-13/2026-99/2026-00),导致 budget.month 字段污染
    // 改用 YearMonth.parse() 严格校验,既保证格式正确(yyyy-MM),又验证月份合法性(1-12)
    if (request.getMonth() == null || !request.getMonth().matches("^\\d{4}-\\d{2}$")) {  // 第一道防线:正则校验格式
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "月份格式必须为 yyyy-MM");  // 抛出参数非法异常
    }
    try {
      java.time.YearMonth.parse(request.getMonth());  // 第二道防线:语义校验(2026-13 / 2026-00 这类非法月份在此被拦截)
    } catch (java.time.format.DateTimeParseException e) {  // YearMonth.parse 对非法月份抛出 DateTimeParseException
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "月份非法,月份必须在 01-12 之间: " + request.getMonth());  // 抛出参数非法异常
    }

    // R-05-issue-6: 已修复 - 捕获DuplicateKeyException并发兜底
    // 两层并发处理策略：第一层先查询→不存在则insert；并发时唯一索引拦截→捕获DuplicateKeyException→
    // 重新查询已存在记录并update。极端场景（记录被并发删除）直接抛异常让用户重试。
    // 查询是否已存在该月该分类的预算
    Budget existing = budgetMapper.selectOne(  // 查询同用户同分类同月的预算
        new LambdaQueryWrapper<Budget>()
            .eq(Budget::getUserId, userId)  // 筛选当前用户
            .eq(Budget::getCategoryId, request.getCategoryId())  // 筛选分类
            .eq(Budget::getMonth, request.getMonth())  // 筛选月份
    );

    Budget budget;  // 最终预算实体
    if (existing != null) {  // 预算已存在
      // 更新
      budget = existing;  // 复用已有实体
      budget.setAmount(request.getAmount());  // 更新金额
      budget.setUpdateTime(LocalDateTime.now());  // 更新修改时间
      budgetMapper.updateById(budget);  // 写入数据库
    } else {  // 预算不存在，新增
      // 新增
      budget = buildBudgetEntity(userId, request);  // 创建预算实体（提取公共方法）
      try {
        budgetMapper.insert(budget);  // 插入数据库
      } catch (DuplicateKeyException e) {  // 并发插入冲突
        // 并发插入冲突: 唯一索引拦截，重新查询已存在记录并更新
        log.warn("预算并发插入冲突，回退更新: userId={}, categoryId={}, month={}", userId, request.getCategoryId(), request.getMonth());  // 记录并发冲突日志
        budget = budgetMapper.selectOne(  // 重新查询已存在记录
            new LambdaQueryWrapper<Budget>()
                .eq(Budget::getUserId, userId)  // 筛选用户
                .eq(Budget::getCategoryId, request.getCategoryId())  // 筛选分类
                .eq(Budget::getMonth, request.getMonth())  // 筛选月份
        );
        if (budget == null) {  // 极端场景：记录被并发删除，直接抛异常让用户重试
          throw new BusinessException(ErrorCode.BUDGET_SAVE_CONFLICT.getCode(), ErrorCode.BUDGET_SAVE_CONFLICT.getMsg());  // 并发冲突抛异常
        }
        // 查到了已存在记录，更新金额
        budget.setAmount(request.getAmount());  // 更新金额
        budget.setUpdateTime(LocalDateTime.now());  // 更新修改时间
        budgetMapper.updateById(budget);  // 写入数据库
      }
    }

    BudgetDTO dto = new BudgetDTO();  // 创建返回DTO
    dto.setId(budget.getId());  // 设置预算ID
    dto.setCategoryId(budget.getCategoryId());  // 设置分类ID
    dto.setCategoryName(category.getName());  // 设置分类名称(复用已加载的category)
    dto.setMonth(budget.getMonth());  // 设置月份
    dto.setAmount(budget.getAmount());  // 设置金额
    dto.setCreateTime(budget.getCreateTime());  // 设置创建时间
    dto.setUpdateTime(budget.getUpdateTime());  // 设置更新时间
    return dto;  // 返回DTO
  }

  /**
   * 删除预算
   *
   * <p>对应 PRD P1-3 预算删除。</p>
   * <p>校验: 预算归属当前用户, 物理删除。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param budgetId 预算ID
   * @throws BusinessException 4005=预算不存在
   */
  @Override
  @Transactional
  public void delete(Long userId, Long budgetId) {  // 删除预算
    Budget budget = budgetMapper.selectById(budgetId);  // 根据ID查询预算
    // 校验：预算存在 + 归属当前用户（Integer用Objects.equals比较值，避免引用比较bug和null userId NPE）
    if (budget == null || !Objects.equals(budget.getUserId(), userId)) {
      throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND.getCode(), ErrorCode.BUDGET_NOT_FOUND.getMsg());  // 抛出业务异常
    }
    budgetMapper.deleteById(budgetId);  // 物理删除预算
  }

  /**
   * 获取预算消耗进度（含已用金额/百分比/超支标记）
   *
   * <p>对应 PRD P1-3 GET /api/budget/progress。</p>
   * <p>性能优化(R-05-issue-2): 将selectCategorySummary提到循环外, 一次性查询所有分类的支出汇总,
   * 再在内存中聚合, 消除N+1查询。</p>
   * <p>计算公式: percentage = (spentAmount / budgetAmount) × 100%, overspent = spentAmount > budgetAmount。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份(如"2026"), 空时默认当前年
   * @param month 月份(如"5"), 空时默认当前月
   * @return 各分类预算进度列表(含预算金额/已用金额/百分比/是否超支)
   */
  @Override
  @Transactional(readOnly = true)
  public List<BudgetProgressDTO> getProgress(Long userId, String year, String month) {
    // 统一通过 EntityValidator null-defaulting + 格式验证
    String monthStr = EntityValidator.defaultAndFormatYearMonth(year, month);  // 格式化年月(空值默认当前)
    // 使用 EntityValidator 安全解析方法替代脆弱的 substring+indexOf
    int yearInt = EntityValidator.extractYear(monthStr);  // 提取年份整数
    int monthInt = EntityValidator.extractMonth(monthStr);  // 提取月份整数

    // 直接查询Budget实体（避免调用list()产生的冗余category批量查询）
    List<Budget> budgets = budgetMapper.selectList(  // 查询用户该月预算列表
        new LambdaQueryWrapper<Budget>()
            .eq(Budget::getUserId, userId)  // 筛选当前用户
            .eq(Budget::getMonth, monthStr)  // 筛选指定月份
    );

    // 批量加载分类名称（仅一次DB查询）
    Set<Long> categoryIds = budgets.stream().map(Budget::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());  // 收集分类ID集合
    Map<Long, String> categoryNameMap = categoryIds.isEmpty()  // 批量查询分类名称
        ? Map.of()  // 无分类ID则返回空Map
        : categoryMapper.selectByIds(categoryIds).stream()  // 一次性查询所有分类
            .collect(Collectors.toMap(Category::getId, Category::getName));  // 分类ID→名称映射

    // R-05-issue-2: 已修复 - selectCategorySummary提到循环外消除N+1查询 · 范围查询利用 idx_transaction_user_time 索引
    // 复用 EntityValidator.buildMonthRange() 消除重复代码
    String[] monthRange = EntityValidator.buildMonthRange(yearInt, monthInt);  // 构建月份范围时间字符串（复用EntityValidator公共方法）
    List<CategorySummaryDTO> summaryList = transactionMapper.selectCategorySummary(userId, monthRange[0], monthRange[1], TransactionType.EXPENSE.getValue());  // 批量查询各分类支出汇总（调用 TransactionMapper.java 的 selectCategorySummary 方法）
    // null安全防护：XML mapper在无数据时可能返回null（与 BudgetAlertProcessorServiceImpl.processOneUser 第97行 null检查一致）
    Map<Long, BigDecimal> spentMap = (summaryList != null ? summaryList : List.<CategorySummaryDTO>of()).stream()  // 分类支出汇总转Map（null→空列表）
        .collect(Collectors.toMap(
            CategorySummaryDTO::getCategoryId,  // 分类ID作key
            CategorySummaryDTO::getTotalAmount,  // 总支出作value
            (a, b) -> a  // 重复key取第一个
        ));

    List<BudgetProgressDTO> result = new ArrayList<>();  // 预算进度结果列表
    for (Budget budget : budgets) {  // 遍历每条预算
      BudgetProgressDTO dto = new BudgetProgressDTO();  // 创建进度DTO
      dto.setCategoryId(budget.getCategoryId());  // 设置分类ID
      dto.setCategoryName(categoryNameMap.getOrDefault(budget.getCategoryId(), ""));  // 设置分类名称
      dto.setBudgetAmount(budget.getAmount());  // 设置预算金额

      BigDecimal spent = spentMap.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);  // 获取该分类实际支出(默认0)
      dto.setSpentAmount(spent);  // 设置已用金额

      // 计算百分比 · setScale(2) 限制小数位数避免无限小数
      if (budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {  // 预算金额大于0
        // 提取中间变量提升可读性，便于调试时查看每步计算结果
        BigDecimal ratio = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP);  // 已用/预算比例（保留4位防精度丢失）
        BigDecimal percentage = ratio.multiply(PERCENTAGE_FACTOR).setScale(2, RoundingMode.HALF_UP);  // 比例×100=百分比
        dto.setPercentage(percentage);  // 设置百分比
      } else {  // 预算金额为0
        dto.setPercentage(BigDecimal.ZERO);  // 百分比设为0
      }

      // 是否超支
      dto.setOverspent(spent.compareTo(budget.getAmount()) > 0);  // 已用>预算则超支

      result.add(dto);  // 加入结果列表
    }
    return result;  // 返回所有预算进度
  }

  /**
   * 构建预算实体（提取公共方法，消除save()中重复创建逻辑）
   *
   * @param userId  当前用户ID
   * @param request 预算请求参数
   * @return 新建的预算实体（未插入数据库）
   */
  private Budget buildBudgetEntity(Long userId, BudgetRequest request) {  // 构建预算实体
    Budget budget = new Budget();  // 创建预算实体
    budget.setUserId(userId);  // 设置用户ID
    budget.setCategoryId(request.getCategoryId());  // 设置分类ID
    budget.setMonth(request.getMonth());  // 设置月份
    budget.setAmount(request.getAmount());  // 设置金额
    budget.setCreateTime(LocalDateTime.now());  // 设置创建时间
    budget.setUpdateTime(LocalDateTime.now());  // 设置更新时间
    return budget;  // 返回预算实体
  }

  /**
   * 获取预算预警列表（仅返回超支项）
   *
   * <p>对应 PRD P2-2 GET /api/budget/alert。</p>
   * <p>复用getProgress()结果, 过滤overspent=true的项。</p>
   * <p>教学简化: 当前仅返回超支项, 日预警(日均150%)和月预警(月耗80%)由BudgetScheduler定时任务日志输出。</p>
   *
   * @param userId 当前用户ID(从JWT token解析)
   * @param year 年份, 空时默认当前年
   * @param month 月份, 空时默认当前月
   * @return 超支的分类预算列表(空集合表示无预警)
   */
  @Override
  @Transactional(readOnly = true)  // MT-1 修复：添加事务注解确保 Spring AOP 代理生效，getProgress() 内部 DB 操作在统一事务上下文中执行
  public List<BudgetProgressDTO> getAlert(Long userId, String year, String month) {  // 获取超支预警（复用getProgress()结果过滤超支项）
    List<BudgetProgressDTO> all = getProgress(userId, year, month);  // → this.getProgress()（同Service内调用 · 外层@Transactional(readOnly=true)覆盖所有DB操作）
    return all.stream()  // 过滤超支项
        .filter(BudgetProgressDTO::isOverspent)  // 仅保留超支项
        .toList();  // 转为不可变列表
  }
}
