package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.ExchangeRateDTO;
import com.example.finance.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 汇率控制器（PRD P2 附加特色功能 · 硬编码参考汇率 · 可替换为外部 API）
 *
 * 职责：仅接收 HTTP 请求 + 委托 ExchangeRateService 处理业务逻辑
 * 路由前缀：/api/v1/exchange-rate
 *
 * 接口清单：
 *   GET /api/v1/exchange-rate — 获取参考汇率（6 种货币正向+反向汇率）
 */
@RestController
@RequestMapping("/api/v1/exchange-rate")
@RequiredArgsConstructor
@Validated
public class ExchangeRateController {

  /** → ExchangeRateService：汇率业务逻辑层 */
  private final ExchangeRateService exchangeRateService;

  /**
   * 获取参考汇率接口（P2 硬编码占位实现）
   *
   * @return Result<ExchangeRateDTO> 汇率数据
   */
  @GetMapping
  public Result<ExchangeRateDTO> getExchangeRates() {
    return Result.success(exchangeRateService.getExchangeRates());
  }
}