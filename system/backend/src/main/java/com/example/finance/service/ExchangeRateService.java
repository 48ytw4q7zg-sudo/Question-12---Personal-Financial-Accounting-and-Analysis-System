package com.example.finance.service;

import com.example.finance.entity.dto.ExchangeRateDTO;

/**
 * 汇率服务接口（P2 硬编码参考汇率 · 可替换为外部 API）
 *
 * 职责：提供 6 种常用货币对 CNY 的参考汇率（硬编码，非实时）
 * 调用方：ExchangeRateController（/api/exchange-rate 路由）
 */
public interface ExchangeRateService {

  /**
   * 获取参考汇率数据
   *
   * @return ExchangeRateDTO 汇率数据（含 rates/ratesInverse/count/source/base/updateTime）
   */
  ExchangeRateDTO getExchangeRates();
}