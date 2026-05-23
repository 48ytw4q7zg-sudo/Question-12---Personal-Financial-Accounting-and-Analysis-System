package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.BudgetAlertDTO;
import com.example.finance.entity.dto.BudgetDTO;
import com.example.finance.entity.dto.BudgetProgressDTO;
import com.example.finance.entity.dto.BudgetQueryRequest;
import com.example.finance.entity.dto.BudgetRequest;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.BudgetAlertService;
import com.example.finance.service.BudgetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预算控制器（PRD P1-3 预算管理：月预算按分类设置 + 超支标记）
 *
 * 职责：接收预算管理的 HTTP 请求，参数校验后转发 BudgetService 处理
 * 路由前缀：/api/v1/budget
 * 依赖：→ BudgetService（业务逻辑层）→ BudgetMapper + TransactionMapper（数据访问层）
 *
 * 接口清单：
 *   GET    /api/budget           — 查询预算列表（按年月筛选）
 *   POST   /api/budget           — 保存预算（新增或更新，INSERT ON DUPLICATE KEY UPDATE）
 *   DELETE /api/v1/budget/{id}      — 删除预算
 *   GET    /api/budget/progress  — 查询预算进度（含已支出金额、百分比、超支标记）
 *   GET    /api/budget/alert     — 查询预算预警（仅超支项）
 *
 * 被前端调用：→ api/budget.js 的 getBudgetList/saveBudget/getBudgetProgress/getBudgetAlert
 * 被 BudgetPage.vue 调用
 *
 * 定时预警：→ BudgetScheduler 每日 2:00 自动检查日/月阈值（详见 scheduler/BudgetScheduler.java）
 */
@RestController
@RequestMapping("/api/v1/budget")
@RequiredArgsConstructor
@Validated
public class BudgetController {

  /** → BudgetService：处理预算列表/保存/删除/进度的业务逻辑 */
  private final BudgetService budgetService;
  /** → BudgetAlertService：处理预算预警查询的业务逻辑（P2-2 @Scheduled 持久化预警） */
  private final BudgetAlertService budgetAlertService;

  /**
   * 查询预算列表（按年月筛选）
   *
   * @param query   年月查询参数（BudgetQueryRequest 含 year + month，由 @Valid 校验）
   * @param request HTTP 请求（LoginInterceptor 已注入 userId 属性）
   * @return Result<List<BudgetDTO>> 该月所有分类的预算列表
   *
   * 被前端 BudgetPage.vue 的月度预算表格调用
   */
  @GetMapping
  public Result<List<BudgetDTO>> list(@Valid BudgetQueryRequest query,
      HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    List<BudgetDTO> list = budgetService.list(userId, query.getYear(), query.getMonth());
    return Result.success(list);
  }

  /**
   * 保存预算（新增或覆盖更新，同一用户+同一月+同一分类唯一）
   *
   * @param request  预算请求体（含 categoryId + amount，由 @Valid 校验金额>0 且分类为支出类）
   * @param httpRequest HTTP 请求（LoginInterceptor 已注入 userId 属性）
   * @return Result<BudgetDTO> 保存后的预算数据
   *
   * 被前端 BudgetPage.vue 的「保存」按钮调用
   */
  @PostMapping
  public Result<BudgetDTO> save(@Valid @RequestBody BudgetRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    BudgetDTO budget = budgetService.save(userId, request);
    return Result.success(budget, "预算保存成功");
  }

  /**
   * 删除预算
   *
   * @param id       预算主键 ID
   * @param httpRequest HTTP 请求（LoginInterceptor 已注入 userId 属性）
   * @return Result<Void> 删除结果
   *
   * 被前端 BudgetPage.vue 的「删除」按钮调用
   */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable @Min(1) Long id, HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    budgetService.delete(userId, id);
    return Result.success(null, "预算已删除");
  }

  /**
   * 查询预算进度（含已支出金额、百分比、超支标记）
   *
   * @param query   年月查询参数（BudgetQueryRequest 含 year + month）
   * @param request HTTP 请求（LoginInterceptor 已注入 userId 属性）
   * @return Result<List<BudgetProgressDTO>> 每个支出分类的预算进度（含 spentAmount / budgetAmount / percentage）
   *
   * 被前端 BudgetPage.vue 的进度条 + DashboardPage.vue 的预警区域调用
   */
  @GetMapping("/progress")
  public Result<List<BudgetProgressDTO>> getProgress(@Valid BudgetQueryRequest query,
      HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    List<BudgetProgressDTO> list = budgetService.getProgress(userId, query.getYear(), query.getMonth());
    return Result.success(list);
  }

  /**
   * 查询预算预警（P2-2 多维度预警：日预警/月预警/超支/正常）
   *
   * @param query   年月查询参数（BudgetQueryRequest 含 year + month）
   * @param request HTTP 请求（LoginInterceptor 已注入 userId 属性）
   * @return Result<List<BudgetAlertDTO>> 预警状态列表（含 alertLevel / categoryName / budgetAmount / spentAmount / percentage）
   *
   * 被前端 DashboardPage.vue 的预警提示区域 + BudgetPage.vue 的预警标签调用
   */
  @GetMapping("/alert")
  public Result<List<BudgetAlertDTO>> getAlert(@Valid BudgetQueryRequest query,
      HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    List<BudgetAlertDTO> list = budgetAlertService.getAlerts(userId, query.getYear(), query.getMonth());
    return Result.success(list);
  }
}