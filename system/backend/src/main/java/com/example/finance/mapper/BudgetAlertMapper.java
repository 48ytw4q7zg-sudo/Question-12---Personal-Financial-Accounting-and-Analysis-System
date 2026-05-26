package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.BudgetAlert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预算预警 Mapper — 映射 budget_alert 表（MyBatis-Plus BaseMapper，内置 CRUD · P2-2）
 *
 * <p>预算预警记录由 BudgetScheduler 每日凌晨 2:00 定时生成（scheduler/BudgetScheduler.java），
 * 由 BudgetController GET /api/budget/alert 查询展示。</p>
 * <p>每次执行先 delete（按 userId + month 删旧记录，幂等），再 insert 新预警记录。</p>
 *
 * <p>调用方:</p>
 * <ul>
 *   <li>BudgetScheduler.checkBudgetAlerts() — delete(清理旧预警) + 委托 BudgetAlertProcessorService 逐用户 insert(写入新预警 · scheduler/BudgetScheduler.java)</li>
 *   <li>BudgetAlertServiceImpl.getAlerts() — selectList(按 userId+month 查询预警列表 · service/impl/BudgetAlertServiceImpl.java)</li>
 *   <li>BudgetAlertProcessorServiceImpl.processUserBudgetAlerts() — delete(分用户删旧) + insert(逐条写新 · service/impl/BudgetAlertProcessorServiceImpl.java)</li>
 *   <li>AdminServiceImpl.deleteUser() — delete(按 userId 级联清理 · service/impl/AdminServiceImpl.java)</li>
 * </ul>
 *
 * <p>budget_alert 表索引: idx_budget_alert_user_month(user_id, month)</p>
 */
@Mapper
public interface BudgetAlertMapper extends BaseMapper<BudgetAlert> {
}
