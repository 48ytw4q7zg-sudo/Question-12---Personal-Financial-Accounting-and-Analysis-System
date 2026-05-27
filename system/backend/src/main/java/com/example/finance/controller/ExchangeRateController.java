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
 * <p>职责：仅接收 HTTP 请求 + 委托 ExchangeRateService 处理业务逻辑。</p>
 * <p>路由前缀：/api/v1/exchange-rate</p>
 * <p>依赖：→ ExchangeRateService（汇率业务逻辑层，硬编码 6 种货币参考汇率 · service/impl/ExchangeRateServiceImpl.java）</p>
 * <p>被前端调用：→ api/exchange-rate.js 的 getExchangeRates()（AccountPage.vue 多币种余额 CNY 等值换算使用）</p>
 * <p>被后端间接调用：AccountServiceImpl.getBalance() 通过注入的 ExchangeRateService 获取汇率用于余额换算</p>
 *
 * <p>接口清单:</p>
 * <pre>
 *   GET /api/v1/exchange-rate — 获取参考汇率（6 种货币正向+反向汇率 · source="static-reference"）
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/exchange-rate")
@RequiredArgsConstructor
@Validated
public class ExchangeRateController {

  /** → ExchangeRateService：汇率业务逻辑层（硬编码 6 种货币参考汇率 · 正向+反向 · service/impl/ExchangeRateServiceImpl.java） */
  private final ExchangeRateService exchangeRateService;

  /**
   * 获取参考汇率接口（P2 硬编码占位实现 · 可替换为外部 API 调用）
   *
   * <p>流程：委托 ExchangeRateService.getExchangeRates() 返回 6 种货币的正向+反向汇率。</p>
   * <ul>
   *   <li>正向汇率 rates：1 CNY → X 外币（如 USD 0.1379）</li>
   *   <li>反向汇率 ratesInverse：1 外币 → X CNY（自动计算 1/rate，确保数学一致性，如 USD→CNY≈7.2504）</li>
   * </ul>
   * <p>使用场景：AccountPage.vue 外帀账户余额自动换算为 CNY 等值金额展示。</p>
   *
   * <p>调用链: 前端 AccountPage.vue → api/exchange-rate.js getExchangeRates() → ExchangeRateController.getExchangeRates() → ExchangeRateService.getExchangeRates()</p>
   * <p>后端间接调用: AccountServiceImpl.getBalance() 通过 @Autowired ExchangeRateService 获取 Map<String,BigDecimal> ratesInverse 用于余额换算</p>
   *
   * @return Result&lt;ExchangeRateDTO&gt; 汇率数据（含 source/base/rates/ratesInverse/count/updateTime 字段）
   */
  @GetMapping
  public Result<ExchangeRateDTO> getExchangeRates() {
    // → service/impl/ExchangeRateServiceImpl.java getExchangeRates()：从硬编码 Map 读取 6 种货币正向+反向汇率
    return Result.success(exchangeRateService.getExchangeRates());
  }
}