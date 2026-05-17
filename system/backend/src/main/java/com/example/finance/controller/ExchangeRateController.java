package com.example.finance.controller;

import com.example.finance.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 汇率控制器（P2，硬编码参考汇率 · 可替换为外部 API）
 */
@RestController
@RequestMapping("/api/exchange-rate")
public class ExchangeRateController {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * 获取参考汇率（P2 硬编码占位实现 · 后续可接入 https://api.exchangerate-api.com 等实时源）
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
    data.put("updateTime", LocalDateTime.now().format(FMT));
    return Result.success(data);
  }
}
