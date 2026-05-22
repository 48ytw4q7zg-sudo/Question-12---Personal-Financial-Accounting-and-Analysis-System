package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.RecurringBillDTO;
import com.example.finance.entity.dto.RecurringBillRequest;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.RecurringBillService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 周期性账单控制器（PRD P1-4 周期性账单提醒）
 *
 * 职责：接收周期性账单管理的 HTTP 请求，参数校验后转发 RecurringBillService 处理
 * 路由前缀：/api/recurring-bill
 * 依赖：→ RecurringBillService（业务逻辑层）→ RecurringBillMapper + AccountMapper + TransactionMapper
 *
 * 接口清单：
 *   GET    /api/recurring-bill                — 查询周期性账单列表
 *   POST   /api/recurring-bill                — 创建周期性账单
 *   PUT    /api/recurring-bill/{id}           — 更新周期性账单
 *   DELETE /api/recurring-bill/{id}           — 停用周期性账单（软删除，status 置 0）
 *   POST   /api/recurring-bill/{id}/generate  — 手动触发生成交易记录
 *
 * 被前端调用：→ api/recurring-bill.js 的 list/create/update/deactivate/generate
 * 被 RecurringBillPage.vue 调用
 *
 * 教学简化：账单生成由用户手动触发（POST /{id}/generate），BudgetScheduler 不自动生成交
 * 易记录（BudgetScheduler 仅负责预算预警检查）。
 */
@RestController
@RequestMapping("/api/recurring-bill")
@RequiredArgsConstructor
@Validated
public class RecurringBillController {

  /** → RecurringBillService：处理周期账单 CRUD + 生成交易记录的业务逻辑 */
  private final RecurringBillService recurringBillService;

  /**
   * 查询周期性账单列表接口
   *
   * 流程：按 userId 查询活跃账单（status=1）→ JOIN account/category 获取名称
   *     → 如果关联账户已禁用，在列表中标记异常状态
   *
   * @param request HTTP 请求
   * @return Result<List<RecurringBillDTO>> 周期账单列表（含关联账户/分类名）
   *
   * 被前端 RecurringBillPage.vue 列表展示调用
   */
  @GetMapping
  public Result<List<RecurringBillDTO>> list(HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → RecurringBillService.list()：查询所有账单 + 关联账户/分类信息
    List<RecurringBillDTO> list = recurringBillService.list(userId);
    return Result.success(list);
  }

  /**
   * 创建周期性账单接口
   *
   * 流程：@Valid 校验名称/金额/周期/账户/分类 → 校验账户状态正常
   *     → 计算下次到期日 → 插入 recurring_bill 表
   *
   * @param request 周期账单请求体（name/accountId/categoryId/amount/type/period，含 @Valid 校验）
   * @param httpRequest HTTP 请求
   * @return Result<RecurringBillDTO> 新创建的周期账单
   *
   * 被前端 RecurringBillPage.vue 新建弹窗调用
   * period 可选值：monthly=每月 / weekly=每周
   * 业务异常码：5006 = 账户不存在 / 5002 = 关联账户已禁用
   */
  @PostMapping
  public Result<RecurringBillDTO> create(@Valid @RequestBody RecurringBillRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → RecurringBillService.create()：校验账户状态 + 计算下次到期日 + 插入数据库
    RecurringBillDTO bill = recurringBillService.create(userId, request);
    return Result.success(bill, "周期性账单创建成功");
  }

  /**
   * 更新周期性账单接口
   *
   * 流程：校验账单归属权 → 更新字段（金额/周期/下次到期日等）
   *
   * @param id 周期账单 ID（URL 路径参数）
   * @param request 周期账单更新请求体
   * @param httpRequest HTTP 请求
   * @return Result<RecurringBillDTO> 更新后的周期账单
   *
   * 被前端 RecurringBillPage.vue 编辑弹窗调用
   */
  @PutMapping("/{id}")
  public Result<RecurringBillDTO> update(@PathVariable @Min(1) Long id,
      @Valid @RequestBody RecurringBillRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → RecurringBillService.update()：校验归属权 + 更新数据库
    RecurringBillDTO bill = recurringBillService.update(userId, id, request);
    return Result.success(bill, "周期性账单更新成功");
  }

  /**
   * 停用周期性账单接口（软删除，status 置 0，不可恢复）
   *
   * 流程：校验归属权 → 条件 UPDATE（WHERE status=1）→ affectedRows 判断是否重复停用
   *
   * @param id 周期账单 ID（URL 路径参数）
   * @param request HTTP 请求
   * @return Result<Void> 成功无返回数据
   *
   * 被前端 RecurringBillPage.vue 停用按钮调用
   * 并发安全：使用条件 UPDATE + affectedRows 判断防重复停用
   */
  @DeleteMapping("/{id}")
  public Result<Void> deactivate(@PathVariable @Min(1) Long id, HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → RecurringBillService.deactivate()：条件 UPDATE status=0 WHERE id=? AND status=1
    recurringBillService.deactivate(userId, id);
    return Result.success(null, "周期性账单已停用");
  }

  /**
   * 手动生成交易记录接口（从周期账单模板生成收支记录）
   *
   * 流程：校验账单状态=活跃 → 校验关联账户状态=正常
   *     → @Transactional：条件更新 next_due_date（防重复生成）→ 插入 transaction 表
   *     → 计算下一个到期日并更新账单
   *
   * @param id 周期账单 ID（URL 路径参数）
   * @param request HTTP 请求
   * @return Result<TransactionDTO> 生成的交易记录
   *
   * 被前端 RecurringBillPage.vue「生成」按钮调用
   * 业务异常码：5004 = 账单已停用 / 5002 = 关联账户已禁用
   */
  @PostMapping("/{id}/generate")
  public Result<TransactionDTO> generate(@PathVariable @Min(1) Long id, HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → RecurringBillService.generate()：
    //   1. 校验账单 status=1 + 关联账户 status=1
    //   2. @Transactional：条件更新 next_due_date 防重复 + 插入 transaction
    //   3. 计算下一个到期日（monthly 加 1 月 / weekly 加 7 天）
    TransactionDTO transaction = recurringBillService.generate(userId, id);
    return Result.success(transaction, "交易记录已生成");
  }
}
