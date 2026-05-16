package com.example.finance.controller;

import com.example.finance.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 汇率控制器（P2，简单硬编码实现）
 */
@RestController
@RequestMapping("/api/exchange-rate")
public class ExchangeRateController {

  /**
   * 获取汇率（硬编码）
   */
  // R-05-issue-3: 低 - 业务逻辑直接写在Controller，无Service层，P2硬编码可接受但应加TODO
  @GetMapping
  public Result<Map<String, Object>> getExchangeRates() {
    Map<String, Object> data = new HashMap<>();
    data.put("CNY_USD", new BigDecimal("0.1370"));
    data.put("CNY_EUR", new BigDecimal("0.1260"));
    data.put("CNY_JPY", new BigDecimal("20.50"));
    data.put("CNY_GBP", new BigDecimal("0.1080"));
    data.put("USD_CNY", new BigDecimal("7.30"));
    data.put("EUR_CNY", new BigDecimal("7.94"));
    data.put("JPY_CNY", new BigDecimal("0.0488"));
    data.put("GBP_CNY", new BigDecimal("9.26"));
    data.put("updateTime", "2026-05-16 00:00:00");
    return Result.success(data);
  }
}
