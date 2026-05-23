package com.example.finance.common;

import com.example.finance.common.enums.Status;
import com.example.finance.entity.Account;
import com.example.finance.entity.Category;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 跨 Service 共享的实体校验辅助类 + 日期格式化工具方法
 *
 * <p>职责范围：本类包含两类功能——实体校验（validateAccount/validateCategory）和日期格式化（formatYearMonth/defaultAndFormatYearMonth/extractYear/extractMonth）。</p>
 * <p>设计权衡：虽然类名侧重"EntityValidator"，但日期格式化方法与校验逻辑紧密相关（如 BudgetServiceImpl 中 monthStr 的格式验证），
 * 且均为跨 Service 共享的纯工具方法，因此合并在一个类中，避免创建过多单方法工具类。</p>
 *
 * <p>消除的重复代码：</p>
 * <ul>
 *   <li>AccountServiceImpl / TransactionServiceImpl / RecurringBillServiceImpl 中的 validateAccount / validateCategory 重复</li>
 *   <li>BudgetServiceImpl / BudgetAlertServiceImpl 中的 monthStr 格式化重复</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class EntityValidator {

  private final AccountMapper accountMapper;
  private final CategoryMapper categoryMapper;

  /**
   * 校验账户归属 + 状态活跃，返回账户实体
   *
   * @param userId    当前用户 ID
   * @param accountId 账户 ID
   * @return 账户实体
   * @throws BusinessException 3004 账户不存在或已禁用
   */
  public Account validateAccount(Long userId, Long accountId) {
    Account account = accountMapper.selectById(accountId);  // 根据ID查询账户
    // 校验：账户存在 + 归属当前用户 + 状态为活跃（Integer用Objects.equals比较值，避免引用比较bug）
    if (account == null || !Objects.equals(account.getUserId(), userId) || !Objects.equals(account.getStatus(), Status.ACTIVE.getValue())) {
      throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getCode(), ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getMsg());  // 抛出业务异常
    }
    return account;  // 返回账户实体
  }

  /**
   * 校验分类存在，返回分类实体
   *
   * @param categoryId 分类 ID
   * @return 分类实体
   * @throws BusinessException 3005 分类不存在
   */
  public Category validateCategory(Long categoryId) {
    Category category = categoryMapper.selectById(categoryId);
    if (category == null) {
      throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND.getCode(), ErrorCode.CATEGORY_NOT_FOUND.getMsg());
    }
    return category;
  }

  /**
   * 格式化年月为 yyyy-MM 字符串（消除 BudgetServiceImpl / BudgetAlertServiceImpl 的重复逻辑）
   *
   * P1-12 修复(Q-CR Loop2):新增月份范围校验(1-12),防止 month=13/0/-1 等非法值进入字符串
   *
   * @param year  年份字符串（如 "2026"）
   * @param month 月份字符串（如 "5" 或 "05"，必须在 1-12 范围内）
   * @return 格式化的 yyyy-MM 字符串
   */
  public static String formatYearMonth(String year, String month) {
    // 防御性编程：防止调用方直接传入 null（虽然 defaultAndFormatYearMonth 已处理 null，但此方法为 public 可能被直接调用）
    if (year == null || month == null) {  // 参数null检查
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "年月参数不可为 null: year=" + year + ", month=" + month);  // 抛出参数非法异常
    }
    try {
      int yearInt = Integer.parseInt(year);  // 校验年份可解析为整数
      int monthInt = Integer.parseInt(month);  // 解析月份整数
      // P1-12 修复(Q-CR Loop2):月份语义范围校验,防止 0/13/99 等非法值
      if (monthInt < 1 || monthInt > 12) {  // 月份超出 1-12 合法范围
        throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "月份必须在 01-12 之间: " + month);  // 抛出参数非法异常
      }
      // P1-12 修复(Q-CR Loop2):年份合理性校验(2000-2100),与 StatisticsService.YEAR_MIN/MAX 一致
      if (yearInt < 2000 || yearInt > 2100) {  // 年份超出合理范围
        throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "年份必须在 2000-2100 之间: " + year);  // 抛出参数非法异常
      }
      return String.format("%s-%02d", year, monthInt);  // 格式化为 yyyy-MM
    } catch (NumberFormatException e) {  // 年份或月份格式非法
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "年月格式不合法: year=" + year + ", month=" + month);  // 抛出参数非法异常
    }
  }

  /**
   * 默认化 null 的年月值（null 时填充当前年月），并格式化校验
   * 消除 BudgetServiceImpl / BudgetAlertServiceImpl 的重复 null-defaulting 逻辑
   *
   * @param year  年份字符串（可为 null）
   * @param month 月份字符串（可为 null）
   * @return 格式化后的 yyyy-MM 字符串
   */
  public static String defaultAndFormatYearMonth(String year, String month) {
    if (year == null || month == null) {
      LocalDateTime now = LocalDateTime.now();
      year = String.valueOf(now.getYear());
      month = String.valueOf(now.getMonthValue());
    }
    return formatYearMonth(year, month);
  }

  /**
   * 从 yyyy-MM 格式字符串中安全提取年份数值
   * 使用 split("-") 替代 substring+indexOf，避免 indexOf 返回 -1 时产生异常
   *
   * @param monthStr yyyy-MM 格式字符串
   * @return 年份数值
   * @throws BusinessException 3013=年月格式不合法或包含非数字字符
   */
  public static int extractYear(String monthStr) {
    String[] parts = monthStr.split("-");
    if (parts.length < 2) {
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "年月格式不合法: " + monthStr);
    }
    try {
      return Integer.parseInt(parts[0]);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "年份必须为数字: " + parts[0]);
    }
  }

  /**
   * 从 yyyy-MM 格式字符串中安全提取月份数值
   *
   * @param monthStr yyyy-MM 格式字符串
   * @return 月份数值
   * @throws BusinessException 3013=年月格式不合法或包含非数字字符
   */
  public static int extractMonth(String monthStr) {
    String[] parts = monthStr.split("-");
    if (parts.length < 2) {
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "年月格式不合法: " + monthStr);
    }
    try {
      return Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "月份必须为数字: " + parts[1]);
    }
  }

  /**
   * 构建月份范围时间字符串（月初和下月初，用于范围查询）
   * <p>消除 StatisticsServiceImpl / BudgetServiceImpl / BudgetAlertProcessorServiceImpl 中的重复时间字符串拼接逻辑。</p>
   *
   * @param year  年份
   * @param month 月份(1-12)
   * @return 长度为2的数组，[0]=月初时间字符串，[1]=下月初时间字符串
   */
  public static String[] buildMonthRange(int year, int month) {  // 构建月份范围时间字符串
    String startOfMonth = String.format("%04d-%02d-01 00:00:00", year, month);  // 月初时间字符串
    String startOfNextMonth = (month == 12)  // 下月初时间字符串
        ? String.format("%04d-01-01 00:00:00", year + 1)  // 12月则下一年1月
        : String.format("%04d-%02d-01 00:00:00", year, month + 1);  // 其他月份+1
    return new String[]{startOfMonth, startOfNextMonth};  // 返回范围数组
  }

  /**
   * 构建年份范围时间字符串（年初和下年初，用于范围查询）
   * <p>消除 StatisticsServiceImpl 中的重复时间字符串拼接逻辑。</p>
   *
   * @param year 年份
   * @return 长度为2的数组，[0]=年初时间字符串，[1]=下年初时间字符串
   */
  public static String[] buildYearRange(int year) {  // 构建年份范围时间字符串
    String startOfYear = String.format("%04d-01-01 00:00:00", year);  // 年初时间字符串
    String startOfNextYear = String.format("%04d-01-01 00:00:00", year + 1);  // 下年初时间字符串
    return new String[]{startOfYear, startOfNextYear};  // 返回范围数组
  }
}
