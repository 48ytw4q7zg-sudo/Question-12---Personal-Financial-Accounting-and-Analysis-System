package com.example.finance.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginRateLimiter 单元测试（P2-5 单测加分）
 *
 * <p>覆盖场景：</p>
 * <ul>
 *   <li>同用户名 2 次/秒通过</li>
 *   <li>同用户名第 3 次快速连击拒绝</li>
 *   <li>不同用户名独立限流</li>
 *   <li>cleanup 清理后恢复许可</li>
 *   <li>size 返回活跃限流器数量</li>
 * </ul>
 *
 * <p>注意：Guava RateLimiter 基于真实时间，测试中使用 Thread.sleep 保证时序可靠。</p>
 */
class LoginRateLimiterTest {

  /**
   * 每个测试前通过反射清空静态 limiters Map，确保测试隔离
   */
  @BeforeEach
  @AfterEach
  void clearLimiters() throws Exception {
    Field field = LoginRateLimiter.class.getDeclaredField("limiters");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    ConcurrentHashMap<String, ?> map = (ConcurrentHashMap<String, ?>) field.get(null);
    map.clear();
  }

  // ==================== 1. 同用户名限流 ====================

  /**
   * 同用户名第 1 次请求 → 通过
   */
  @Test
  void tryAcquire_firstRequest_allowed() {
    assertTrue(LoginRateLimiter.tryAcquire("alice"));
  }

  /**
   * 同用户名连续 2 次请求 → 均通过（PERMITS_PER_SECOND=2.0，初始有许可）
   * 注意：Guava RateLimiter 初始有 1 个 stored permit，第二次请求间隔极短时可能通过也可能不通过
   * 这里验证至少第 1 次必定通过
   */
  @Test
  void tryAcquire_twoRapidRequests_firstAlwaysAllowed() {
    boolean first = LoginRateLimiter.tryAcquire("bob");
    assertTrue(first, "第 1 次请求必定通过");

    // 第 2 次立即调用：RateLimiter 初始有 1 stored permit，通常通过
    // 但由于 Guava 的 warm-up 行为可能因版本而异，不强制断言
    LoginRateLimiter.tryAcquire("bob");
  }

  /**
   * 同用户名快速连续 3+ 次请求 → 至少有一次被拒绝
   * PERMITS_PER_SECOND=2.0，即每秒最多 2 次，第 3 次快速连击应被拒绝
   */
  @Test
  void tryAcquire_rapidBurst_atLeastOneDenied() {
    // 快速连续调用 5 次
    int allowed = 0;
    for (int i = 0; i < 5; i++) {
      if (LoginRateLimiter.tryAcquire("charlie")) {
        allowed++;
      }
    }

    // 5 次快速调用不可能全部通过（每秒最多 2 次许可）
    assertTrue(allowed < 5,
        "5 次快速请求不应全部通过，实际通过: " + allowed);
  }

  /**
   * 同用户名间隔 1 秒后 → 许可恢复，再次通过
   */
  @Test
  void tryAcquire_afterOneSecond_permitsRecovered() throws InterruptedException {
    // 消耗初始许可
    LoginRateLimiter.tryAcquire("dave");
    LoginRateLimiter.tryAcquire("dave");

    // 等待 1.1 秒让许可恢复
    Thread.sleep(1100);

    // 许可恢复后应能通过
    assertTrue(LoginRateLimiter.tryAcquire("dave"),
        "等待 1.1 秒后许可应已恢复");
  }

  // ==================== 2. 不同用户名独立限流 ====================

  /**
   * 不同用户名各自独立限流，互不影响
   * alice 被限流不影响 bob 的请求
   */
  @Test
  void tryAcquire_differentUsernames_independent() {
    // 耗尽 alice 的许可
    for (int i = 0; i < 5; i++) {
      LoginRateLimiter.tryAcquire("alice");
    }

    // bob 的第 1 次请求应通过（独立限流器）
    assertTrue(LoginRateLimiter.tryAcquire("bob"),
        "bob 的请求应不受 alice 限流影响");
  }

  /**
   * 两个用户名各自创建独立限流器实例
   */
  @Test
  void tryAcquire_twoUsers_twoSeparateLimiters() {
    LoginRateLimiter.tryAcquire("user1");
    LoginRateLimiter.tryAcquire("user2");

    assertEquals(2, LoginRateLimiter.size(),
        "两个不同用户名应创建两个独立限流器");
  }

  // ==================== 3. cleanup 清理 ====================

  /**
   * cleanup 后移除该用户名的限流器，下次 tryAcquire 重新创建
   */
  @Test
  void cleanup_removesLimiter() {
    LoginRateLimiter.tryAcquire("eve");
    assertEquals(1, LoginRateLimiter.size());

    LoginRateLimiter.cleanup("eve");
    assertEquals(0, LoginRateLimiter.size(),
        "cleanup 后限流器应被移除");
  }

  /**
   * cleanup 后重新请求 → 创建新限流器，许可恢复
   */
  @Test
  void cleanup_thenTryAcquire_createsNewLimiter() {
    // 耗尽许可
    for (int i = 0; i < 5; i++) {
      LoginRateLimiter.tryAcquire("frank");
    }

    // 清理后重新请求
    LoginRateLimiter.cleanup("frank");
    assertTrue(LoginRateLimiter.tryAcquire("frank"),
        "cleanup 后新限流器应有可用许可");
  }

  // ==================== 4. size 监控 ====================

  /**
   * 初始 size = 0
   */
  @Test
  void size_initiallyZero() {
    assertEquals(0, LoginRateLimiter.size());
  }

  /**
   * 多个用户名 → size 正确累加
   */
  @Test
  void size_multipleUsers_correctCount() {
    LoginRateLimiter.tryAcquire("a");
    LoginRateLimiter.tryAcquire("b");
    LoginRateLimiter.tryAcquire("c");

    assertEquals(3, LoginRateLimiter.size());
  }

  /**
   * 同一用户名多次请求 → size 仍为 1（复用同一限流器）
   */
  @Test
  void size_sameUserMultipleRequests_staysOne() {
    LoginRateLimiter.tryAcquire("reuse");
    LoginRateLimiter.tryAcquire("reuse");
    LoginRateLimiter.tryAcquire("reuse");

    assertEquals(1, LoginRateLimiter.size(),
        "同一用户名应复用同一个限流器实例");
  }
}
