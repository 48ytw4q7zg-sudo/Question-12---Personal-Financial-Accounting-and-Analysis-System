package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 账户 Mapper — 映射 account 表（MyBatis-Plus BaseMapper，内置 CRUD + 1 条自定义 SQL）
 *
 * <p>继承 BaseMapper&lt;Account&gt; 提供标准数据访问方法。</p>
 * <p>条件查询走 AccountServiceImpl 中的 LambdaQueryWrapper（eq(userId)+eq(status)+orderByDesc(createTime)）。</p>
 * <p>自定义 SQL: selectByIdForUpdate 用于转账场景的悲观锁。</p>
 *
 * <p>调用方:</p>
 * <ul>
 *   <li>AccountServiceImpl.list() — selectList(查用户活跃账户 · 按创建时间倒序)</li>
 *   <li>AccountServiceImpl.create() — selectCount(同名查重) + insert(创建写入)</li>
 *   <li>AccountServiceImpl.update() — selectById(归属校验) + updateById(更新写入)</li>
 *   <li>AccountServiceImpl.delete() — selectById(归属校验) + updateById(软删除 status=0)</li>
 *   <li>AccountServiceImpl.getBalance() — selectList(全量活跃账户) + selectAccountIncomeBatch/selectAccountExpenseBatch(批量收支查询)</li>
 *   <li>TransactionServiceImpl.transfer() — selectByIdForUpdate(FOR UPDATE 悲观锁 · 并发余额校验)</li>
 *   <li>RecurringBillServiceImpl.create/update() — EntityValidator.validateAccount → selectById(账户存在+归属校验)</li>
 *   <li>RecurringBillServiceImpl.generate() — selectById(关联账户状态校验)</li>
 *   <li>RecurringBillServiceImpl.list() — selectByIds(批量加载账户名称 · 消除 N+1)</li>
 * </ul>
 *
 * <p>account 表索引: idx_account_user_id(user_id) + uk_account_name_user_id(name, user_id)</p>
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

  /** 悲观锁查询：SELECT * FROM account WHERE id = #{id} FOR UPDATE
   *  <p>用途: 转账余额检查时使用，防 TOCTOU 并发透支（InnoDB REPEATABLE READ 隔离级别下，FOR UPDATE 锁住行防止并发送扣）。</p>
   *  <p>调用方: TransactionServiceImpl.transfer()（controller/TransactionController.java → service/impl/TransactionServiceImpl.java 第331行）</p> */
  @Select("SELECT * FROM account WHERE id = #{id} FOR UPDATE")
  Account selectByIdForUpdate(Long id);
}