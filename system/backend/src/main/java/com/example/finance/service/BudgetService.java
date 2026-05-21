package com.example.finance.service;

import com.example.finance.entity.dto.BudgetDTO;
import com.example.finance.entity.dto.BudgetProgressDTO;
import com.example.finance.entity.dto.BudgetRequest;

import java.util.List;

/**
 * 预算服务接口（PRD P1-3 预算管理：按分类设置月预算 + 超支预警）
 */
public interface BudgetService {

  /**
   * 查询用户预算列表（按月份筛选）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param year   年份（yyyy 格式，如 2026）
   * @param month  月份（MM 格式，如 05）
   * @return 预算列表（含分类名称，按分类排序）
   */
  List<BudgetDTO> list(Long userId, String year, String month);

  /**
   * 保存预算（同一用户+同一分类+同一月份唯一，存在则覆盖）
   *
   * @param userId  当前用户 ID（JWT 解码获取）
   * @param request 预算请求（含分类 ID、月份、预算金额）
   * @return 保存后的预算 DTO
   * @throws com.example.finance.common.BusinessException 4003 分类不存在（对齐 ErrorCode.CATEGORY_NOT_FOUND_FOR_BUDGET）/ 4002 预算仅可设置在支出分类上
   */
  BudgetDTO save(Long userId, BudgetRequest request);

  /**
   * 删除预算
   *
   * @param userId   当前用户 ID（JWT 解码获取）
   * @param budgetId 要删除的预算 ID
   * @throws com.example.finance.common.BusinessException 4005 预算不存在（对齐 ErrorCode.BUDGET_NOT_FOUND）
   */
  void delete(Long userId, Long budgetId);

  /**
   * 获取预算进度（已支出 / 预算金额 × 100%）
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param year   年份（yyyy 格式，如 2026）
   * @param month  月份（MM 格式，如 05）
   * @return 预算进度列表（含预算金额、已支出金额、消耗百分比、是否超支）
   */
  List<BudgetProgressDTO> getProgress(Long userId, String year, String month);

  /**
   * 获取预算预警（仅返回已超支的分类项 · 旧版接口）
   *
   * <p>注意：BudgetController 的 GET /api/budget/alert 已改用 BudgetAlertService.getAlerts()
   * 返回 List&lt;BudgetAlertDTO&gt;（含 4 级预警级别），本方法仅返回 overspent=true 的 BudgetProgressDTO。
   * 本方法保留供 BudgetServiceImpl.getProgress() 内部复用，前端请使用 BudgetAlertService。</p>
   *
   * @param userId 当前用户 ID（JWT 解码获取）
   * @param year   年份（yyyy 格式，如 2026）
   * @param month  月份（MM 格式，如 05）
   * @return 超支预算进度列表（spentAmount > budgetAmount 的项）
   */
  List<BudgetProgressDTO> getAlert(Long userId, String year, String month);
}
