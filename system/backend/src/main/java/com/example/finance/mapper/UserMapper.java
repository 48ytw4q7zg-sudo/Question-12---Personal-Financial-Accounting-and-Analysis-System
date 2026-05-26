package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper — 映射 user 表（MyBatis-Plus BaseMapper，内置 CRUD）
 *
 * <p>继承 BaseMapper&lt;User&gt; 提供标准数据访问方法（insert/selectById/selectOne/selectList/updateById/deleteById 等）。</p>
 * <p>条件查询全部走 Service 层的 LambdaQueryWrapper（类型安全 + 防 SQL 注入），不在 Mapper 定义 SQL。</p>
 *
 * <p>调用方:</p>
 * <ul>
 *   <li>UserServiceImpl.register() — selectOne(username 查重) + insert(注册写入)</li>
 *   <li>UserServiceImpl.login() — selectOne(username 查询) + selectById(改密查用户)</li>
 *   <li>UserServiceImpl.changePassword() — selectById(查用户) + updateById(写新密码)</li>
 *   <li>AdminServiceImpl.listAllUserDTOs() — selectList(全量用户列表)</li>
 *   <li>AdminServiceImpl.deleteUser() — selectById(存在校验) + deleteById(硬删除)</li>
 *   <li>AdminServiceImpl.toggleUserRole() — selectById(存在校验) + updateById(写新角色)</li>
 * </ul>
 *
 * <p>user 表唯一约束: uk_username(username) — 注册并发唯一键由 DuplicateKeyException 兜底（见 UserServiceImpl.register() 第111行）</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
