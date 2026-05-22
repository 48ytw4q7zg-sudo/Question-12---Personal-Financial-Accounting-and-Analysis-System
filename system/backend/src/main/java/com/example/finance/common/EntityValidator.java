package com.example.finance.common;

import com.example.finance.common.enums.Status;
import com.example.finance.entity.Account;
import com.example.finance.entity.Category;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 跨 Service 共享的实体校验辅助类 + 日期格式化工具方法
 *
 * 消除 AccountServiceImpl / TransactionServiceImpl / RecurringBillServiceImpl 中
 * validateAccount / validateCategory 的代码重复
 * 消除 BudgetServiceImpl / BudgetAlertServiceImpl 中 monthStr 格式化的代码重复
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
    Account account = accountMapper.selectById(accountId);
    if (account == null || !account.getUserId().equals(userId) || account.getStatus() != Status.ACTIVE.getValue()) {
      throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getCode(), ErrorCode.ACCOUNT_NOT_FOUND_OR_DISABLED.getMsg());
    }
    return account;
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
   * @param year  年份字符串（如 "2026"）
   * @param month 月份字符串（如 "5" 或 "05"）
   * @return 格式化的 yyyy-MM 字符串
   */
  public static String formatYearMonth(String year, String month) {
    try {
      int monthInt = Integer.parseInt(month);
      return String.format("%s-%02d", year, monthInt);
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "年月格式不合法: year=" + year + ", month=" + month);
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
   */
  public static int extractYear(String monthStr) {
    String[] parts = monthStr.split("-");
    return Integer.parseInt(parts[0]);
  }

  /**
   * 从 yyyy-MM 格式字符串中安全提取月份数值
   *
   * @param monthStr yyyy-MM 格式字符串
   * @return 月份数值
   */
  public static int extractMonth(String monthStr) {
    String[] parts = monthStr.split("-");
    return Integer.parseInt(parts[1]);
  }
}
