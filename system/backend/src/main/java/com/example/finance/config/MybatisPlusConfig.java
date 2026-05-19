package com.example.finance.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis-Plus 配置（分页插件 + 事务管理）
 *
 * - PaginationInnerInterceptor：自动识别分页请求，生成 MySQL LIMIT 子句
 * - @EnableTransactionManagement：启用 @Transactional 注解支持
 */
@Configuration
@EnableTransactionManagement
public class MybatisPlusConfig {

  /**
   * MyBatis-Plus 拦截器（当前仅注册 MySQL 分页插件）
   *
   * 分页使用方式：
   *   Page<T> page = new Page<>(pageNum, pageSize);
   *   mapper.selectPage(page, wrapper);  // 自动分页
   * 或手动 XML 分页（本项目 TransactionMapper 使用 RowBounds + 独立 count 查询）
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
  }
}
