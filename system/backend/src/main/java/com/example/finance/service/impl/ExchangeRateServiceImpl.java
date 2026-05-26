package com.example.finance.service.impl;

import com.example.finance.entity.dto.ExchangeRateDTO;
import com.example.finance.service.ExchangeRateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  /** 正向汇率：1 CNY → X 外币（手工维护的参考值 · 近似市场中间价 · 最后更新 2026-05-23 · 后续优化方向：接入实时汇率 API） */
  private static final Map<String, BigDecimal> RATES = Map.of(
      "USD", new BigDecimal("0.1379"),
      "EUR", new BigDecimal("0.1275"),
      "JPY", new BigDecimal("19.8000"),
      "GBP", new BigDecimal("0.1092"),
      "HKD", new BigDecimal("1.0770"),
      "KRW", new BigDecimal("187.2000")
  );

  /**
   * 反向汇率：1 外币 → X CNY（从正向汇率自动计算，确保数学一致性）
   * 计算公式：1/rate，精度 4 位小数，HALF_UP 舍入
   */
  private static final Map<String, BigDecimal> RATES_INVERSE = computeInverseRates();

  /**
   * 从正向汇率自动计算反向汇率（1 外币 → X CNY）
   * 计算公式：1 / rate，精度 4 位小数，HALF_UP 舍入
   * 确保正向汇率修改后反向汇率自动同步，避免手工维护不一致
   *
   * @return 不可变的反向汇率映射（USD→7.2504, EUR→7.8431, ...）
   */
  private static Map<String, BigDecimal> computeInverseRates() {
    Map<String, BigDecimal> inverse = new LinkedHashMap<>();  // 保持插入顺序
    for (Map.Entry<String, BigDecimal> entry : RATES.entrySet()) {  // 遍历所有货币
      // BigDecimal.ONE.divide(rate, 4, HALF_UP)：1 ÷ 正向汇率，保留4位小数，四舍五入
      inverse.put(entry.getKey(), BigDecimal.ONE.divide(entry.getValue(), INVERSE_SCALE, RoundingMode.HALF_UP));  // 计算反向汇率
    }
    return inverse;  // 返回不可变映射
  }

  /**
   * 获取汇率数据（正向 + 反向汇率 · P2-4 多币种换算）
   *
   * <p>返回 ExchangeRateDTO（含 source/base/rates/ratesInverse/count/updateTime）。</p>
   * <ul>
   *   <li>rates: 1 CNY → X 外币（正向汇率，如 CNY→USD=0.1379）</li>
   *   <li>ratesInverse: 1 外币 → X CNY（反向汇率，如 USD→CNY≈7.2504，AccountServiceImpl.getBalance() 余额 CNY 等值换算使用）</li>
   * </ul>
   * <p>反向汇率由 computeInverseRates() 自动从正向汇率计算（1÷rate），确保数学一致性。</p>
   * <p>硬编码占位实现，后续可替换为外部 API 调用。</p>
   *
   * <p>调用链: ExchangeRateController.getExchangeRates() → ExchangeRateService.getExchangeRates()
   *   → 前端 api/exchange-rate.js getExchangeRates() → AccountPage.vue 余额 CNY 等值换算</p>
   * <p>间接调用: AccountServiceImpl.getBalance() 通过注入的 ExchangeRateService 获取汇率用于多币种余额换算</p>
   *
   * @return ExchangeRateDTO 汇率数据对象（source="static-reference" 表示硬编码参考汇率 · base="CNY" · count=6 种外币）
   */
  @Override
  @Transactional(readOnly = true)                    // Q-CR修复：与项目中其他10个ServiceImpl保持一致的事务注解规范
  public ExchangeRateDTO getExchangeRates() {
    // 构造 ExchangeRateDTO 返回 → ExchangeRateController → 前端 AccountPage.vue
    // 参数: source=static-reference, base=CNY, 正向汇率 RATES(6种), 反向汇率 RATES_INVERSE(6种), count=6, updateTime=static
    return new ExchangeRateDTO(SOURCE, BASE_CURRENCY, RATES, RATES_INVERSE, CURRENCY_COUNT, "static");
  }
}