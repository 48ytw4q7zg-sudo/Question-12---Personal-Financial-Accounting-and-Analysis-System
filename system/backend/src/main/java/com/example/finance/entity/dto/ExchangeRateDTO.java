// 数据传输对象包（DTO 层，用于接收前端请求和返回响应）
package com.example.finance.entity.dto;

// Lombok 全参构造器注解（初始化 ExchangeRateDTO 时一次赋值所有字段）
import lombok.AllArgsConstructor;
// Lombok 自动生成 getter/setter/toString/equals/hashCode
import lombok.Data;
// Lombok 无参构造器注解（Jackson 反序列化需要无参构造器）
import lombok.NoArgsConstructor;

// Java 高精度金额类型（汇率精度要求高，禁止 float/double）
import java.math.BigDecimal;
// Java Map 接口（存储货币代码 → 汇率值的键值对）
import java.util.Map;

/**
 * 汇率数据传输对象 DTO（替代原来 raw Map<String, Object> 的返回结构，类型安全）
 *
 * 对应 PRD P2-4 多币种换算: 6 种货币对 CNY 的参考汇率（正向 + 反向）。
 *
 * 字段说明:
 *   - source: 数据来源标识（当前硬编码 "static-reference"，后续可替换为外部 API 名称）
 *   - base: 基准货币（CNY）
 *   - rates: 正向汇率 Map（1 CNY → X 外币，如 "USD" → 0.14）
 *   - ratesInverse: 反向汇率 Map（1 外币 → X CNY，如 "USD" → 7.14，AccountServiceImpl 余额换算使用）
 *   - count: 支持货币数量
 *   - updateTime: 数据更新时间（硬编码值 "static"，外部 API 模式下为实际时间戳）
 *
 * 调用方: controller/StatisticsController.java → service/impl/StatisticsServiceImpl.java
 */
// Lombok: 自动生成 getter/setter/toString/equals/hashCode（Lombok 1.18.46）
@Data
// Lombok: 生成无参构造器（Jackson JSON 反序列化要求 · 先创建空对象再 setter 赋值）
@NoArgsConstructor
// Lombok: 生成全参构造器（Service 层构造 DTO 时一次赋值所有字段）
@AllArgsConstructor
// 汇率信息 DTO 类（Controller 通过 Result<ExchangeRateDTO> 返回前端币种换算组件）
public class ExchangeRateDTO {

  /** 数据来源标识（当前硬编码 "static-reference"，后续切换外部 API 时改为 "openexchangerates" 等） */
  private String source;

  /** 基准货币代码（固定 "CNY" 人民币，所有汇率以此为基准计算） */
  private String base;

  /** 正向汇率 Map（1 CNY → X 外币，key=货币代码如 "USD"/"EUR"/"JPY"，value=BigDecimal 汇率值） */
  private Map<String, BigDecimal> rates;

  /** 反向汇率 Map（1 外币 → X CNY，AccountServiceImpl 多币种余额换算核心数据源，key/value 同上） */
  private Map<String, BigDecimal> ratesInverse;

  /** 支持的货币种类数量（rates.size()，前端展示"支持 N 种货币"） */
  private int count;

  /** 数据更新时间（静态模式为 "static"，API 模式为 ISO 8601 时间戳，如 "2026-05-26T10:00:00Z"） */
  private String updateTime;
}