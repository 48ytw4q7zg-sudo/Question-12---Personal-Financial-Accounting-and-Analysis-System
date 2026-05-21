package com.example.finance.service.impl;

import com.example.finance.service.ExchangeRateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 汇率服务实现（P2 硬编码参考汇率 · 可替换为外部 API）
 *
 * 职责：提供 6 种常用货币对 CNY 的参考汇率
 * 支持货币：USD / EUR / JPY / GBP / HKD / KRW
 * 后续优化方向：接入实时汇率 API，替换硬编码值
 */
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {

  /** 基准货币 */
  private static final String BASE_CURRENCY = "CNY";
  /** 数据来源标识 */
  private static final String SOURCE = "static-reference";
  /** 支持货币数量 */
  private static final int CURRENCY_COUNT = 6;

  /** 正向汇率：1 CNY → X 外币 */
  private static final Map<String, BigDecimal> RATES = Map.of(
      "USD", new BigDecimal("0.1370"),
      "EUR", new BigDecimal("0.1260"),
      "JPY", new BigDecimal("20.5000"),
      "GBP", new BigDecimal("0.1080"),
      "HKD", new BigDecimal("1.0700"),
      "KRW", new BigDecimal("186.5000")
  );

  /** 反向汇率：1 外币 → X CNY */
  private static final Map<String, BigDecimal> RATES_INVERSE = Map.of(
      "USD", new BigDecimal("7.3000"),
      "EUR", new BigDecimal("7.9400"),
      "JPY", new BigDecimal("0.0488"),
      "GBP", new BigDecimal("9.2600"),
      "HKD", new BigDecimal("0.9346"),
      "KRW", new BigDecimal("0.0054")
  );

  @Override
  public Map<String, Object> getExchangeRates() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("source", SOURCE);
    data.put("base", BASE_CURRENCY);
    data.put("rates", RATES);
    data.put("ratesInverse", RATES_INVERSE);
    data.put("count", CURRENCY_COUNT);
    data.put("updateTime", "static");
    return data;
  }
}