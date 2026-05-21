package com.example.finance.util;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录限流工具类（防暴力破解）
 *
 * <p>基于 Guava RateLimiter 实现按用户名粒度的登录限流。</p>
 * <p>每个用户名独立限流：每秒最多 2 次登录尝试，超出则拒绝。</p>
 * <p>使用 ConcurrentHashMap 存储每个用户名的限流器实例，线程安全。</p>
 * <p>可通过 cleanup() 方法手动清理（避免内存泄漏）。</p>
 *
 * <p>答辩高频提问点：</p>
 * <ul>
 *   <li>为什么用 RateLimiter 不用 @RateLimit 注解？→ 教学项目，Guava 更轻量，不引 Spring Boot Starter</li>
 *   <li>为什么按用户名限流不按 IP？→ 教学简化，IP 限流需考虑 NAT 共享场景更复杂</li>
 *   <li>生产环境怎么做？→ Redis + Lua 脚本分布式限流，或 Spring Boot Starter RateLimit</li>
 * </ul>
 */
public class LoginRateLimiter {

  /** 每个用户名每秒最多 2 次登录尝试 */
  private static final double PERMITS_PER_SECOND = 2.0;

  /**
   * 用户名 → RateLimiter 映射（线程安全）
   * <p>教学简化: 每个用户名永久驻留内存，生产环境应使用 Caffeine 缓存 + TTL 自动过期，
   * 或 Redis 分布式限流。当前实现在教学场景(≤20 并发用户)下无内存泄漏风险。</p>
   */
  private static final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

  /**
   * 尝试获取登录许可
   *
   * @param username 用户名
   * @return true=允许登录尝试, false=限流拒绝
   */
  public static boolean tryAcquire(String username) {
    RateLimiter limiter = limiters.computeIfAbsent(username,
        k -> RateLimiter.create(PERMITS_PER_SECOND));
    return limiter.tryAcquire();
  }

  /**
   * 清理指定用户名的限流器（登录成功后可选调用，释放内存）
   *
   * @param username 用户名
   */
  public static void cleanup(String username) {
    limiters.remove(username);
  }

  /**
   * 获取当前活跃的限流器数量（监控/调试用）
   */
  public static int size() {
    return limiters.size();
  }
}
