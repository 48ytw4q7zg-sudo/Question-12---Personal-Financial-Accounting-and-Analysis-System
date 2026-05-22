package com.example.finance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 个人财务记账与分析系统 - 启动类
 *
 * @MapperScan("com.example.finance.mapper") — 自动扫描 Mapper 接口并注册为 Spring Bean，无需每个 Mapper 手动加 @Mapper 注解
 * @EnableScheduling — 启用 Spring 定时任务支持，使 @Scheduled 注解生效（用于 BudgetScheduler 每日预算预警检查）
 */
@SpringBootApplication
@MapperScan("com.example.finance.mapper")
@EnableScheduling
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
