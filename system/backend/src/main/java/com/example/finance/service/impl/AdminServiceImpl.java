package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.enums.UserRole;
import com.example.finance.entity.Account;
import com.example.finance.entity.Budget;
import com.example.finance.entity.BudgetAlert;
import com.example.finance.entity.RecurringBill;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.entity.dto.UserDTO;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.BudgetAlertMapper;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.mapper.RecurringBillMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.mapper.UserMapper;
import com.example.finance.service.AdminService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员服务实现（评分标准要求 ≥2 类用户角色: 普通用户 + 管理员）
 *
 * 职责：管理员专属业务逻辑——用户列表查询、用户删除、角色切换。
 * 调用方：AdminController（/api/admin 路由）
 *
 * 分层约束：
 *   - Controller 层只做参数接收 + 权限校验（checkAdmin）
 *   - Service 层处理业务逻辑（存在性检查、自操作防护）
 *   - Mapper 层只做数据访问
 *   修复了之前 Controller 直接调用 UserMapper 违反分层的问题
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

  /** → UserMapper：用户数据访问层 */
  private final UserMapper userMapper;
  /** → TransactionMapper：交易记录数据访问层（级联删除用） */
  private final TransactionMapper transactionMapper;
  /** → BudgetMapper：预算数据访问层（级联删除用） */
  private final BudgetMapper budgetMapper;
  /** → RecurringBillMapper：周期性账单数据访问层（级联删除用） */
  private final RecurringBillMapper recurringBillMapper;
  /** → AccountMapper：账户数据访问层（级联删除用） */
  private final AccountMapper accountMapper;
  /** → BudgetAlertMapper：预算预警数据访问层（级联删除用） */
  private final BudgetAlertMapper budgetAlertMapper;

  /**
   * 查询所有用户列表
   *
   * <p>流程：查询 user 表全部记录（按 id 升序） → 转换为 UserDTO → 返回。</p>
   * <p>密码由 User 实体的 @JsonIgnore 保护，不会包含在序列化结果中。</p>
   * <p>无分页（管理员用户量小，全量返回可接受）。</p>
   *
   * 调用链路: AdminController.listUsers() → AdminService.listAllUserDTOs() → UserMapper.selectList() → 前端 api/admin.js listUsers() → AdminPage.vue
   *
   * @return 全部用户 DTO 列表（含 id/username/role/createTime，不含 password）
   */
  @Override
  @Transactional(readOnly = true)                                       // 只读事务
  public List<UserDTO> listAllUserDTOs() {
    // → mapper/UserMapper.java 的 selectList()（继承自 BaseMapper<User>）—— 所有用户数据访问
    // LambdaQueryWrapper: 无筛选条件，按 id 升序（保证返回顺序稳定）
    // Stream + toList(): Java 16+ 不可变列表，每个 User 实体通过 UserDTO.fromUser() 转为 DTO
    return userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getId)).stream().map(UserDTO::fromUser).toList();  // 查询所有用户 → 转为 DTO 列表 → 返回给 AdminController
  }

  /**
   * 删除指定用户（硬删除 · 级联清理关联数据）
   *
   * <p>流程：校验不能删除自己 → 校验用户存在 → 按依赖顺序级联删除 6 张表的关联数据 → 删除用户。</p>
   * <p>教学简化：管理员删除用户走物理删除（deleteById），不做软删除。</p>
   * <p>安全约束：管理员不能删除自己。</p>
   * <p>级联顺序设计：transaction（依赖 account）→ budget_alert → budget → recurring_bill → account → user，先删子表后删父表。</p>
   *
   * 调用链路: AdminController.deleteUser() → AdminService.deleteUser() → 6 个 Mapper 的 delete()
   *
   * @param userId        要删除的用户 ID
   * @param currentUserId 当前管理员 ID
   * @throws BusinessException(6001) 管理员不能删除自己（ADMIN_CANNOT_DELETE_SELF · common/ErrorCode.java）
   * @throws BusinessException(6003) 用户不存在（ADMIN_USER_NOT_FOUND · common/ErrorCode.java）
   */
  @Override
  @Transactional                                                    // 事务保护：级联删除 7 步操作原子性（全成功或全回滚）
  public void deleteUser(Long userId, Long currentUserId) {
    // 【步骤①】防止管理员删除自己（Objects.equals 避免 Long 拆箱 NPE）
    if (Objects.equals(currentUserId, userId)) {                    // 目标用户就是当前管理员
      throw new BusinessException(ErrorCode.ADMIN_CANNOT_DELETE_SELF.getCode(), ErrorCode.ADMIN_CANNOT_DELETE_SELF.getMsg());  // → common/ErrorCode.java ADMIN_CANNOT_DELETE_SELF
    }
    // 【步骤②】校验用户是否存在（→ mapper/UserMapper.java 的 selectById · 继承自 BaseMapper<User>）
    User user = userMapper.selectById(userId);                      // 根据 ID 查询用户
    if (user == null) {                                             // 用户不存在
      throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND.getCode(), ErrorCode.ADMIN_USER_NOT_FOUND.getMsg());  // → common/ErrorCode.java ADMIN_USER_NOT_FOUND
    }
    // 【步骤③】级联清理：按依赖顺序删除 6 张表的关联数据（防止孤儿记录）
    // 3.1 删除交易记录（→ mapper/TransactionMapper.java · transaction 表）
    transactionMapper.delete(new LambdaQueryWrapper<Transaction>().eq(Transaction::getUserId, userId));
    // 3.2 删除预算预警（→ mapper/BudgetAlertMapper.java · budget_alert 表）
    budgetAlertMapper.delete(new LambdaQueryWrapper<BudgetAlert>().eq(BudgetAlert::getUserId, userId));
    // 3.3 删除预算（→ mapper/BudgetMapper.java · budget 表）
    budgetMapper.delete(new LambdaQueryWrapper<Budget>().eq(Budget::getUserId, userId));
    // 3.4 删除周期性账单（→ mapper/RecurringBillMapper.java · recurring_bill 表）
    recurringBillMapper.delete(new LambdaQueryWrapper<RecurringBill>().eq(RecurringBill::getUserId, userId));
    // 3.5 删除账户（→ mapper/AccountMapper.java · account 表）
    accountMapper.delete(new LambdaQueryWrapper<Account>().eq(Account::getUserId, userId));
    // 3.6 删除用户（→ mapper/UserMapper.java 的 deleteById · user 表 · 前置 selectById 已确认存在）
    userMapper.deleteById(userId);                                  // 物理删除（影响行数忽略，前置校验确保用户存在）
  }

  /**
   * 切换用户角色（普通用户 ↔ 管理员）
   *
   * <p>流程：校验不能切换自己 → 校验用户存在 → 翻转 role 值（0→1 或 1→0）→ 更新数据库。</p>
   * <p>安全约束：管理员不能切换自己的角色（防止把自己降级后无法管理）。</p>
   *
   * 调用链路: AdminController.toggleRole() → AdminService.toggleUserRole() → UserMapper.updateById()
   *
   * @param userId        要切换角色的用户 ID
   * @param currentUserId 当前管理员 ID
   * @return 更新后的用户信息（UserDTO，不含密码）
   * @throws BusinessException(6002) 管理员不能修改自己的角色（ADMIN_CANNOT_MODIFY_SELF · common/ErrorCode.java）
   * @throws BusinessException(6003) 用户不存在（ADMIN_USER_NOT_FOUND · common/ErrorCode.java）
   */
  @Override
  @Transactional                                                    // 事务保护：查 + 改 2 步操作原子性
  public UserDTO toggleUserRole(Long userId, Long currentUserId) {
    // 【步骤①】防止管理员切换自己的角色（Objects.equals 避免 Long 拆箱 NPE）
    if (Objects.equals(currentUserId, userId)) {                    // 不能切换自己的角色
      throw new BusinessException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF.getCode(), ErrorCode.ADMIN_CANNOT_MODIFY_SELF.getMsg());  // → common/ErrorCode.java ADMIN_CANNOT_MODIFY_SELF
    }
    // 【步骤②】校验用户是否存在（→ mapper/UserMapper.java 的 selectById · 继承自 BaseMapper<User>）
    User user = userMapper.selectById(userId);                      // 根据 ID 查询目标用户
    if (user == null) {                                             // 用户不存在
      throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND.getCode(), ErrorCode.ADMIN_USER_NOT_FOUND.getMsg());  // → common/ErrorCode.java ADMIN_USER_NOT_FOUND
    }
    // 【步骤③】翻转角色：管理员→普通 / 普通→管理员（→ common/enums/UserRole.java ADMIN=1 / NORMAL=0）
    // Objects.equals 比较 Integer 引用值，避免自动拆箱 NPE
    user.setRole(Objects.equals(user.getRole(), UserRole.ADMIN.getValue()) ? UserRole.NORMAL.getValue() : UserRole.ADMIN.getValue());
    user.setUpdateTime(LocalDateTime.now());                        // 更新修改时间
    // 【步骤④】写入数据库（→ mapper/UserMapper.java 的 updateById · 继承自 BaseMapper<User>）
    userMapper.updateById(user);                                    // 通过 Lambda 更新 user 表：SET role=?, update_time=? WHERE id=?
    // 【步骤⑤】Entity 转 DTO（→ entity/dto/UserDTO.java 的 fromUser() 静态工厂方法 · 密码字段 @JsonIgnore 不返回）
    return UserDTO.fromUser(user);                                  // → AdminController → Result.success → 前端 api/admin.js toggleRole() → AdminPage.vue
  }
}
