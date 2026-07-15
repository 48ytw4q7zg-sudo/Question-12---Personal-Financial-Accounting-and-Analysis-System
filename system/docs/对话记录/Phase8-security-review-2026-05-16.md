# Phase 8 · 安全审查记录 (R-08)

日期: 2026-05-16
审查范围: OWASP Top 10 安全维度

## 审查结果

### 1. SQL 注入 ✅ 安全
- 全部使用 MyBatis-Plus LambdaQueryWrapper 参数化查询
- XML 映射使用 #{} 参数化 (非 ${} 拼接)
- TransactionMapper.xml 所有 SQL 条件使用 #{}

### 2. XSS ✅ 安全
- 前端 Vue 模板默认转义 {{ }} 插值
- 后端 @Valid + @Size 限制输入长度
- 无 innerHTML/v-html 使用

### 3. 认证/授权 ✅ 安全
- LoginInterceptor 拦截所有业务接口
- 白名单仅 /login /register /health
- JWT token 7天过期
- 每个接口校验 userId 归属

### 4. 敏感数据 ✅ 安全
- 密码 BCrypt 加密存储
- User entity @JsonIgnore 防泄露
- 日志不打印密码/token (log.debug 脱敏)
- JWT secret 支持环境变量覆盖

### 5. CORS ✅ 安全
- CorsConfig 配置允许的 origin/methods/headers
- credentials: true 限制跨域

### 6. 输入校验 ✅ 安全
- Controller 入参 @Valid + @NotBlank/@Size
- 前端 el-form rules 校验
- 文件上传校验 (.csv + 大小限制)

### 7. 会话管理 ✅ 安全
- JWT 无状态，localStorage 存储
- 401 自动清理 token + 跳转登录
- logout 清理 localStorage

### 8. 错误处理 ✅ 安全
- GlobalExceptionHandler 统一处理
- 不暴露堆栈信息给前端
- 业务异常返回友好提示

## 结论
无高危安全问题。JWT secret 已从硬编码改为可配置。
