# 提示词演化记录 v1 → v2

## v1（初始版本）
> 帮我做一个个人财务记账系统，前后端分离，SpringBoot + Vue

**问题**: 缺少具体功能描述、技术版本约束、数据库设计要求

## v2（优化版本）
> 基于选题标定卡，搭建个人财务记账与分析系统 P0 功能：
> - 后端: SpringBoot 3.5.14 + MyBatis-Plus 3.5.15 + MySQL 8.4 + JWT + BCrypt
> - 前端: Vue 3.5 + Element Plus 2.13.7 + Pinia 3.0.4 + Axios
> - P0: 登录/JWT、账户CRUD、分类列表、收支记录、余额汇总
> - 统一 Result<T> 响应，LambdaQueryWrapper 查询
> - 按 Vertical Slice 模式逐个功能实现

**改进点**: 加入了技术栈版本锁定、P0 优先级分层、明确的编码规范
