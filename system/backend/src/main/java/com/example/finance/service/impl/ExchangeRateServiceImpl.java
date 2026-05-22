package com.example.finance.service.impl;

import com.example.finance.entity.dto.ExchangeRateDTO;
import com.example.finance.service.ExchangeRateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 汇率服务实现（P2 硬编码参考汇率 · 可替换为外部 API）
 *
 * 职责：提供 6 种常用货币对 CNY 的参考汇率
 * 支持货币：USD / EUR / JPY / GBP / HKD / KRW
 * 反向汇率从正向汇率自动计算（1/rate），确保数学一致性
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
  /** 反向汇率精度（4 位小数） */
  private static final int INVERSE_SCALE = 4;

  /** 正向汇率：1 CNY → X 外币（手工维护的参考值） */
  private static final Map<String, BigDecimal> RATES = Map.of(
      "USD", new BigDecimal("0.1370"),
      "EUR", new BigDecimal("0.1260"),
      "JPY", new BigDecimal("20.5000"),
      "GBP", new BigDecimal("0.1080"),
      "HKD", new BigDecimal("1.0700"),
      "KRW", new BigDecimal("186.5000")
  );

  /**
   * 反向汇率：1 外币 → X CNY（从正向汇率自动计算，确保数学一致性）
   * 计算公式：1/rate，精度 4 位小数，HALF_UP 舍入
   */
  private static final Map<String, BigDecimal> RATES_INVERSE = computeInverseRates();

  private static Map<String, BigDecimal> computeInverseRates() {
    Map<String, BigDecimal> inverse = new LinkedHashMap<>();
    for (Map.Entry<String, BigDecimal> entry : RATES.entrySet()) {
      inverse.put(entry.getKey(), BigDecimal.ONE.divide(entry.getValue(), INVERSE_SCALE, RoundingMode.HALF_UP));
    }
    return inverse;
  }

  /**
   * 获取汇率数据（正向 + 反向汇率 · P2-4 多币种换算）
   *
   * 返回 ExchangeRateDTO（含 source/base/rates/ratesInverse/count/updateTime）
   * rates: 1 CNY → X 外币（正向汇率）
   * ratesInverse: 1 外币 → X CNY（反向汇率，AccountServiceImpl 余额换算使用）
   *
   * @return ExchangeRateDTO 汇率数据对象
   */
  @Override
  public ExchangeRateDTO getExchangeRates() {
    return new ExchangeRateDTO(SOURCE, BASE_CURRENCY, RATES, RATES_INVERSE, CURRENCY_COUNT, "static");
  }
}