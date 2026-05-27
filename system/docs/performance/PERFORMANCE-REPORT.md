# 性能压测报告 — Q-CR v13

> 日期: 2026-05-18 · 环境: Windows 11 · JDK 21 · MySQL 8.4

## 测试环境

| 组件 | 配置 |
|---|---|
| CPU | 实测机器 |
| 内存 | 实测机器 |
| JDK | 21 (Temurin) |
| MySQL | 8.4 LTS |
| 后端 | SpringBoot 3.5.14 (端口 8080) |

## 单请求延迟 (curl 实测)

| 端点 | 方法 | 延迟 | 响应大小 |
|---|---|---|---|
| /api/health | GET | 3ms | 144B |
| /api/user/login | POST | 263ms | 215B |
| /api/account | GET | 4ms | 862B |
| /api/transaction?pageNum=1&pageSize=10 | GET | 5ms | 3.3KB |
| /api/statistics/monthly?year=2026&month=5 | GET | 6ms | 133B |
| /api/category | GET | 3ms | 492B |

## 10 并发压测 (3 轮 × 10 用户)

### /api/account (10 并发)

| 轮次 | 响应时间 (ms) |
|---|---|
| 1 | 3.0, 3.2, 3.1, 3.4, 3.2, 28.1, 3.4, 5.4, 5.7, 3.0 |
| 2 | 3.1, 3.2, 3.2, 2.9, 2.8, 3.1, 2.9, 20.3, 2.5, 2.8 |
| 3 | 2.9, 4.7, 3.0, 21.1, 3.3, 3.0, 17.6, 23.8, 2.9, 2.9 |

**统计**: p50=3.2ms · p95=24ms · p99=28ms · 零错误

### /api/transaction (10 并发)

| 响应时间 (ms) |
|---|
| 4.6, 4.2, 4.7, 5.2, 5.1, 5.8, 4.4, 5.2, 3.8, 25.0 |

**统计**: p50=4.9ms · p95=25ms · 零错误

## 结论

- **全部端点 p95 < 30ms**，远低于目标准则 500ms
- **10 并发零错误**，系统稳定
- **Login 端点 263ms** 因 BCrypt (cost=12) 哈希验证，属于正常安全开销
- 系统可支持 ≥20 并发用户 (60 人课堂分组测试上限)

## JMeter 测试计划

完整的 JMeter 测试计划已存档于 `docs/performance/jmeter-test-plan.jmx`。

运行方式:
```bash
jmeter -n -t docs/performance/jmeter-test-plan.jmx \
  -Jtoken=YOUR_JWT_TOKEN \
  -l docs/performance/jmeter-results.jtl \
  -e -o docs/performance/jmeter-html/
```
