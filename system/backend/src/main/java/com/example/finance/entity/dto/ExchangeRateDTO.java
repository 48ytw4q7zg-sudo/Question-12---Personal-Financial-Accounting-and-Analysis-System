package com.example.finance.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 汇率数据传输对象（替代原来 raw Map<String, Object> 的返回结构）
 *
 * <p>对应 PRD P2-4 多币种换算: 6 种货币对 CNY 的参考汇率（正向 + 反向）。</p>
 *
 * <p>字段说明:</p>
 * <ul>
 *   <li>source: 数据来源标识（当前硬编码 "static-reference", 后续可替换为外部 API 名称）</li>
 *   <li>base: 基准货币（CNY）</li>
 *   <li>rates: 正向汇率 Map（1 CNY → X 外币）</li>
 *   <li>ratesInverse: 反向汇率 Map（1 外币 → X CNY, AccountServiceImpl 余额换算使用）</li>
 *   <li>count: 支持货币数量</li>
 *   <li>updateTime: 数据更新时间（硬编码值 "static", 外部 API 模式下为实际时间戳）</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDTO {

  /** 数据来源标识 */
  private String source;

  /** 基准货币 */
  private String base;

  /** 正向汇率：1 CNY → X 外币 */
  private Map<String, BigDecimal> rates;

  /** 反向汇率：1 外币 → X CNY */
  private Map<String, BigDecimal> ratesInverse;

  /** 支持货币数量 */
  private int count;

  /** 数据更新时间 */
  private String updateTime;
}