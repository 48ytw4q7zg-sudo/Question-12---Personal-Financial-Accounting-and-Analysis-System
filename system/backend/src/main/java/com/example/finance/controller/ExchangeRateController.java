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
 * 依赖：→ ExchangeRateService（汇率业务逻辑层，硬编码 6 种货币参考汇率）
 * 被前端调用：→ api/exchange-rate.js 的 getExchangeRates()（AccountPage.vue 多币种余额换算使用）
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
   * 流程：委托 ExchangeRateService.getExchangeRates() 返回 6 种货币的正向+反向汇率
   * 正向汇率：1 CNY → X 外币（如 USD 0.1379）
   * 反向汇率：1 外币 → X CNY（自动计算 1/rate，确保数学一致性）
   * 被前端 AccountPage.vue 的余额 CNY 等值换算区域调用
   *
   * @return Result<ExchangeRateDTO> 汇率数据（含 source/base/rates/ratesInverse/count/updateTime 字段）
   */
  @GetMapping
  public Result<ExchangeRateDTO> getExchangeRates() {
    // → ExchangeRateService.getExchangeRates()：获取硬编码参考汇率（6种货币正向+反向）
    return Result.success(exchangeRateService.getExchangeRates());
  }
}