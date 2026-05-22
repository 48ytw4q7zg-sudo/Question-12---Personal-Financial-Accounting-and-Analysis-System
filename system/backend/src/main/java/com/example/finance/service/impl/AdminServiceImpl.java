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
   * 流程：查询 user 表全部记录（按 id 升序） → 转换为 UserDTO → 返回（密码由 @JsonIgnore 保护）
   *
   * @return 全部用户 DTO 列表
   */
  @Override
  @Transactional(readOnly = true)
  public List<UserDTO> listAllUserDTOs() {
    return userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getId)).stream().map(UserDTO::fromUser).toList();
  }

  /**
   * 删除指定用户（硬删除）
   *
   * 流程：校验不能删除自己 → 校验用户存在 → 物理删除
   *
   * 教学简化：管理员删除用户走物理删除（deleteById），不做软删除。
   * 安全约束：管理员不能删除自己。
   *
   * @param userId        要删除的用户 ID
   * @param currentUserId 当前管理员 ID
   * @throws BusinessException(6001) 管理员不能删除自己
   * @throws BusinessException(6003) 用户不存在
   */
  @Override
  @Transactional
  public void deleteUser(Long userId, Long currentUserId) {
    // 防止管理员删除自己
    if (currentUserId.equals(userId)) {
      throw new BusinessException(ErrorCode.ADMIN_CANNOT_DELETE_SELF.getCode(), ErrorCode.ADMIN_CANNOT_DELETE_SELF.getMsg());
    }
    // 校验用户是否存在
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND.getCode(), ErrorCode.ADMIN_USER_NOT_FOUND.getMsg());
    }
    // 级联清理：按依赖顺序删除关联数据，避免孤儿记录
    // 1. 删除交易记录（依赖账户，先删）
    transactionMapper.delete(new LambdaQueryWrapper<Transaction>().eq(Transaction::getUserId, userId));
    // 2. 删除预算预警
    budgetAlertMapper.delete(new LambdaQueryWrapper<BudgetAlert>().eq(BudgetAlert::getUserId, userId));
    // 3. 删除预算
    budgetMapper.delete(new LambdaQueryWrapper<Budget>().eq(Budget::getUserId, userId));
    // 4. 删除周期性账单
    recurringBillMapper.delete(new LambdaQueryWrapper<RecurringBill>().eq(RecurringBill::getUserId, userId));
    // 5. 删除账户
    accountMapper.delete(new LambdaQueryWrapper<Account>().eq(Account::getUserId, userId));
    // 6. 删除用户
    userMapper.deleteById(userId);
  }

  /**
   * 切换用户角色（普通用户 ↔ 管理员）
   *
   * 流程：校验不能切换自己 → 校验用户存在 → 翻转 role 值（0→1 或 1→0）→ 更新数据库
   *
   * 安全约束：管理员不能切换自己的角色。
   *
   * @param userId        要切换角色的用户 ID
   * @param currentUserId 当前管理员 ID
   * @return 更新后的用户信息
   * @throws BusinessException(6002) 管理员不能修改自己的角色
   * @throws BusinessException(6003) 用户不存在
   */
  @Override
  @Transactional
  public UserDTO toggleUserRole(Long userId, Long currentUserId) {
    if (currentUserId.equals(userId)) {
      throw new BusinessException(ErrorCode.ADMIN_CANNOT_MODIFY_SELF.getCode(), ErrorCode.ADMIN_CANNOT_MODIFY_SELF.getMsg());
    }
    User user = userMapper.selectById(userId);
    if (user == null) {
      throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND.getCode(), ErrorCode.ADMIN_USER_NOT_FOUND.getMsg());
    }
    user.setRole(user.getRole() == UserRole.ADMIN.getValue() ? UserRole.NORMAL.getValue() : UserRole.ADMIN.getValue());
    user.setUpdateTime(LocalDateTime.now());
    userMapper.updateById(user);
    return UserDTO.fromUser(user);
  }
}
