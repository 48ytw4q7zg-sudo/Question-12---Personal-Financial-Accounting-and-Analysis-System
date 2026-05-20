package com.example.finance.controller;

import com.example.finance.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 汇率控制器（PRD P2 附加特色功能 · 硬编码参考汇率 · 可替换为外部 API）
 *
 * 职责：提供 6 种常用货币对 CNY 的参考汇率（硬编码，非实时）
 * 路由前缀：/api/exchange-rate
 * 无 Service/Mapper 依赖（独立实现，后续可接入 exchangerate-api.com 等实时源）
 *
 * 接口清单：
 *   GET /api/exchange-rate — 获取参考汇率（6 种货币正向+反向汇率）
 *
 * 被前端调用：目前无独立页面（附加功能，可在 AccountPage 余额卡片中展示外币等值）
 *
 * 支持货币：USD / EUR / JPY / GBP / HKD / KRW
 */
@RestController
@RequestMapping("/api/exchange-rate")
public class ExchangeRateController {

  /**
   * 获取参考汇率接口（P2 硬编码占位实现）
   *
   * 返回结构：
   *   source      — "static-reference" 表示静态参考值
   *   base        — 基准货币 "CNY"
   *   rates       — 1 CNY → X 外币（如 USD=0.1370）
   *   ratesInverse — 1 外币 → X CNY（如 USD=7.3000）
   *   count       — 支持货币数量（6）
   *   updateTime  — 固定时间戳（硬编码汇率对应固定时间，非实时更新）
   *
   * @return Result<Map<String, Object>> 汇率数据
   *
   * 后续优化方向：接入实时汇率 API，替换硬编码值
   */
  @GetMapping
  public Result<Map<String, Object>> getExchangeRates() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("source", "static-reference");
    data.put("base", "CNY");
    data.put("rates", Map.of(
        "USD", new BigDecimal("0.1370"),
        "EUR", new BigDecimal("0.1260"),
        "JPY", new BigDecimal("20.5000"),
        "GBP", new BigDecimal("0.1080"),
        "HKD", new BigDecimal("1.0700"),
        "KRW", new BigDecimal("186.5000")
    ));
    data.put("ratesInverse", Map.of(
        "USD", new BigDecimal("7.3000"),
        "EUR", new BigDecimal("7.9400"),
        "JPY", new BigDecimal("0.0488"),
        "GBP", new BigDecimal("9.2600"),
        "HKD", new BigDecimal("0.9346"),
        "KRW", new BigDecimal("0.0054")
    ));
    data.put("count", 6);
    // 硬编码静态汇率，updateTime 使用固定时间戳（避免误导为实时更新）
    data.put("updateTime", "2026-05-20 00:00:00");
    return Result.success(data);
  }
}
