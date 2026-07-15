package com.example.finance.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 数据源配置校验（对齐 JwtConfig / CorsConfig 的生产环境安全策略）
 *
 * <p>生产环境启动时检测数据库默认凭证(root/root)，阻止应用启动。</p>
 * <p>开发环境允许默认凭证方便本地调试，仅输出 WARN 日志提醒。</p>
 *
 * <p>调用方：Spring Boot @PostConstruct 自动执行，与其他 config 类安全校验对齐。</p>
 */
@Slf4j
@Configuration
public class DataSourceConfig {

  /** 默认数据库用户名（开发环境占位值） */
  private static final String DEFAULT_DB_USERNAME = "root";
  /** 默认数据库密码（开发环境占位值） */
  private static final String DEFAULT_DB_PASSWORD = "root";

  @Value("${spring.datasource.username}")
  private String dbUsername;

  @Value("${spring.datasource.password}")
  private String dbPassword;

  /** 当前激活的 Spring profile（用于区分开发/生产环境） */
  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  /**
   * 启动时校验数据库凭证安全性
   *
   * <p>检测逻辑：</p>
   * <ul>
   *   <li>生产环境(prod profile)：检测到默认 root/root 凭证 → 抛 IllegalStateException 阻止启动</li>
   *   <li>开发环境：仅输出 WARN 日志提醒</li>
   * </ul>
   *
   * @throws IllegalStateException 生产环境使用默认数据库凭证时阻止启动
   */
  @PostConstruct
  public void validateDataSourceCredentials() {
    boolean usesDefaultCredentials = DEFAULT_DB_USERNAME.equals(dbUsername) && DEFAULT_DB_PASSWORD.equals(dbPassword);
    if (usesDefaultCredentials && "prod".equalsIgnoreCase(activeProfile)) {
      throw new IllegalStateException(
          "⚠️ 检测到数据库使用默认凭证(root/root)，生产环境必须通过 DB_USERNAME / DB_PASSWORD 环境变量替换为强凭证，应用拒绝启动");
    }
    if (usesDefaultCredentials) {
      log.warn("⚠️ 检测到数据库使用默认凭证(root/root)（当前 profile={}），开发环境允许使用但生产环境必须替换！请设置 DB_USERNAME / DB_PASSWORD 环境变量", activeProfile);
    }
  }
}