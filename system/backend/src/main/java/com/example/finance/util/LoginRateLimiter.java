package com.example.finance.util;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录限流工具类（防暴力破解 + TTL 自动过期防内存泄漏）
 *
 * <p>基于 Guava RateLimiter 实现按用户名粒度的登录限流。</p>
 * <p>每个用户名独立限流：每秒最多 2 次登录尝试，超出则拒绝。</p>
 * <p>使用 ConcurrentHashMap 存储每个用户名的限流器实例，线程安全。</p>
 *
 * <p>v12.2 优化：添加 TTL 自动过期机制，10 分钟无活动的限流器自动移除，
 * 防止暴力攻击使用大量虚构用户名导致内存无限增长。</p>
 *
 * <p>答辩高频提问点：</p>
 * <ul>
 *   <li>为什么用 RateLimiter 不用 @RateLimit 注解？→ 教学项目，Guava 更轻量，不引 Spring Boot Starter</li>
 *   <li>为什么按用户名限流不按 IP？→ 教学简化，IP 限流需考虑 NAT 共享场景更复杂</li>
 *   <li>生产环境怎么做？→ Redis + Lua 脚本分布式限流，或 Spring Boot Starter RateLimit</li>
 *   <li>TTL 过期策略？→ 最后访问时间 + 10 分钟无活动自动清除，兼顾安全与内存效率</li>
 * </ul>
 */
public class LoginRateLimiter {

  /** 每个用户名每秒最多 2 次登录尝试 */
  private static final double PERMITS_PER_SECOND = 2.0;
  /** TTL 过期阈值：10 分钟（毫秒），超过此时间无活动的限流器自动移除 */
  private static final long TTL_MS = 10 * 60 * 1000L;

  /**
   * 用户名 → 限流器包装映射（线程安全）
   * <p>每个包装记录最后访问时间，用于 TTL 过期判定。</p>
   */
  private static final ConcurrentHashMap<String, LimiterEntry> limiters = new ConcurrentHashMap<>();

  /**
   * 限流器包装类（RateLimiter + 最后访问时间）
   */
  private static class LimiterEntry {
    final RateLimiter limiter;
    volatile long lastAccessTime;

    LimiterEntry(RateLimiter limiter) {
      this.limiter = limiter;
      this.lastAccessTime = System.currentTimeMillis();
    }

    void touch() {
      this.lastAccessTime = System.currentTimeMillis();
    }
  }

  /**
   * 尝试获取登录许可
   *
   * @param username 用户名
   * @return true=允许登录尝试, false=限流拒绝
   */
  public static boolean tryAcquire(String username) {
    // 定期清理过期条目（每次访问时检查，低频操作不影响性能）
    evictExpired();
    LimiterEntry entry = limiters.computeIfAbsent(username,
        k -> new LimiterEntry(RateLimiter.create(PERMITS_PER_SECOND)));
    entry.touch();
    return entry.limiter.tryAcquire();
  }

  /**
   * 清理指定用户名的限流器（登录成功后调用，释放内存）
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

  /**
   * TTL 自动过期：移除超过 TTL_MS 无活动的条目
   * <p>在 tryAcquire() 内部调用，保证过期条目不会无限累积。</p>
   * <p>遍历成本低（教学项目 ≤20 并发用户），生产环境应改用 Caffeine 缓存自动过期。</p>
   */
  private static void evictExpired() {
    long now = System.currentTimeMillis();
    limiters.entrySet().removeIf(e -> now - e.getValue().lastAccessTime > TTL_MS);
  }
}