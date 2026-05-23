package com.example.finance.service.impl;

// MyBatis-Plus 条件构造器：Lambda 方式构建类型安全的查询条件（避免字符串字段名拼写错误）
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
// Category 实体：映射 category 表（13 条种子数据：支出 8 条 + 收入 5 条，由 sql/01-init.sql 初始化）
import com.example.finance.entity.Category;
// CategoryDTO：分类数据传输对象（仅含 id/name/type 三个字段，不含 createTime/updateTime）
import com.example.finance.entity.dto.CategoryDTO;
// CategoryMapper：分类数据访问层（继承 BaseMapper<Category>，仅提供只读查询 selectList / selectByIds）
import com.example.finance.mapper.CategoryMapper;
// CategoryService 接口：定义分类查询契约（Controller 依赖此接口，面向接口编程）
import com.example.finance.service.CategoryService;
// Lombok 注解：自动生成含所有 final 字段的构造器，实现 Spring 推荐的构造器注入
import lombok.RequiredArgsConstructor;
// Spring 注解：标记为 Service 层 Bean，纳入 IoC 容器统一管理
import org.springframework.stereotype.Service;
// Spring 事务注解：readOnly=true 可提示 MySQL 优化只读查询（路由只读副本、减少锁开销）
import org.springframework.transaction.annotation.Transactional;

// Spring Cache 注解：标记方法返回值可缓存到 ConcurrentMapCacheManager 默认内存缓存
import org.springframework.cache.annotation.Cacheable;
// Java 集合框架：List 接口
import java.util.List;

/**
 * 分类服务实现（PRD P0-3 收支分类查询）
 *
 * <p>分类为种子数据（13 条：支出 8 + 收入 5），由 sql/01-init.sql 初始化。
 * 所有登录用户共享同一套分类体系，不支持用户自定义增改删。</p>
 *
 * <p>被以下 5 个前端页面/组件复用（分类下拉选择 / 分类筛选 / 图表图例）：</p>
 * <ul>
 *   <li>CategoryPage — 分类展示页（只读列表）</li>
 *   <li>TransactionListPage — 交易记录页（分类筛选下拉框）</li>
 *   <li>BudgetPage — 预算管理页（按分类设置/查看预算）</li>
 *   <li>RecurringBillPage — 周期性账单页（关联分类选择器）</li>
 *   <li>AnalyticsPage — 分析图表页（分类饼图 / 收支趋势图例）</li>
 * </ul>
 *
 * <p>性能优化：使用 @Cacheable 缓存全量分类数据（13 条静态数据常驻内存，减少数据库往返）。</p>
 *
 * @see CategoryMapper      分类数据访问层
 * @see CategoryService     分类服务接口
 * @see Category            分类实体
 */
@Service                                                       // 注册为 Spring Service Bean，纳入 IoC 容器
@RequiredArgsConstructor                                      // Lombok：自动生成含 categoryMapper 的构造器（构造器注入）
public class CategoryServiceImpl implements CategoryService {

  /** CategoryMapper：分类数据访问层，继承 BaseMapper<Category>，仅提供只读查询（selectList / selectByIds） */
  private final CategoryMapper categoryMapper;                // final 确保不可变，由 @RequiredArgsConstructor 通过构造器注入

  /**
   * 查询所有分类（种子数据，全量返回，无分页无筛选）
   *
   * <p>业务流程：① 查询 category 表全量记录（按 id 升序） → ② Entity 逐个转为 DTO → ③ 返回 DTO 列表。</p>
   * <p>缓存策略：Spring 默认 ConcurrentMapCacheManager 内存缓存，13 条种子数据首次查询后常驻内存，
   * 后续请求直接从缓存读取，不再访问数据库。</p>
   *
   * @return 分类 DTO 列表（按 id 升序排列，每个 DTO 含 id/name/type 三个字段）
   */
  @Override
  @Transactional(readOnly = true)                             // 只读事务：MySQL 跳过事务锁和 undo log 开销，提升查询性能
  // P1-3 修复(Q-CR Loop1)：unless 条件防止缓存 null 或空列表
  // 旧实现 @Cacheable 无 unless,如启动早期 DB 连接异常返回空列表会被缓存,导致后续永远拿不到种子数据(必须重启)
  // 加 unless 后:空集合不缓存,下次调用会重新查询 DB,实现"自愈"
  @Cacheable(value = "categories", unless = "#result == null || #result.isEmpty()")  // 缓存结果(空列表不缓存,实现自愈)
  public List<CategoryDTO> list() {
    // 【步骤①】使用 LambdaQueryWrapper 构建类型安全的排序查询，查询 category 表全量记录
    List<Category> categories = categoryMapper.selectList(    // → CategoryMapper.selectList：查询全量分类（无筛选条件）
        new LambdaQueryWrapper<Category>()                    // Lambda 条件构造器：字段引用编译期校验，杜绝字符串拼写错误
            .orderByAsc(Category::getId)                      // 按主键 id 升序排列（保证返回顺序稳定）
    );
    // 【步骤②】Stream 遍历 → Entity 逐个转为 DTO → 收集为列表
    return categories.stream().map(c -> {                     // Stream 遍历分类实体列表
      CategoryDTO dto = new CategoryDTO();                    // 新建 DTO 实例
      dto.setId(c.getId());                                   // 映射：分类 ID（主键，自增）
      dto.setName(c.getName());                               // 映射：分类名称（如"餐饮"、"交通"、"工资"）
      dto.setType(c.getType());                               // 映射：收支类型（1=收入 / 2=支出，对齐 DATABASE_DESIGN 约定）
      return dto;                                             // 返回填充好的 DTO
    }).toList();                                              // Java 16+ 收集为不可变列表（替代 Collectors.toList()）
  }
}
