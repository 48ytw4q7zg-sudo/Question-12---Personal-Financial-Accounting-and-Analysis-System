package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.RecurringBill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 周期性账单 Mapper — 映射 recurring_bill 表（MyBatis-Plus BaseMapper，内置 CRUD）
 *
 * <p>继承 BaseMapper&lt;RecurringBill&gt; 提供标准数据访问方法，无需自定义 SQL。</p>
 * <p>条件查询走 RecurringBillServiceImpl 中的 LambdaQueryWrapper（按 userId + status 筛选）。</p>
 * <p>更新走 BaseMapper.updateById()（status 软删除 / 编辑字段 / 下次到期日推进）。</p>
 *
 * <p>调用方:</p>
 * <ul>
 *   <li>RecurringBillServiceImpl.list() — selectList(按 userId+status=1 查活跃账单 · 按创建时间倒序)</li>
 *   <li>RecurringBillServiceImpl.create() — insert(创建新账单 · 含账户/分类/周期/到期日)</li>
 *   <li>RecurringBillServiceImpl.update() — selectById(归属校验) + updateById(更新字段)</li>
 *   <li>RecurringBillServiceImpl.deactivate() — selectById(归属校验) + updateById(软删除 status=0)</li>
 *   <li>RecurringBillServiceImpl.generate() — selectById(归属校验) + updateById(推进到期日)</li>
 *   <li>AccountServiceImpl.delete() — selectCount(检查活跃账单引用 · 防级联删除)</li>
 * </ul>
 *
 * <p>recurring_bill 表索引: idx_recurring_bill_user_id(user_id)</p>
 */
@Mapper
public interface RecurringBillMapper extends BaseMapper<RecurringBill> {
}
