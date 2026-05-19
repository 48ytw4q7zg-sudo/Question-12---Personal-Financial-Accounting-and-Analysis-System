package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.BudgetDTO;
import com.example.finance.entity.dto.BudgetProgressDTO;
import com.example.finance.entity.dto.BudgetRequest;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.BudgetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预算控制器（PRD P1-3 预算管理：月预算按分类设置 + 超支标记）
 *
 * 职责：接收预算管理的 HTTP 请求，参数校验后转发 BudgetService 处理
 * 路由前缀：/api/budget
 * 依赖：→ BudgetService（业务逻辑层）→ BudgetMapper + TransactionMapper（数据访问层）
 *
 * 接口清单：
 *   GET  /api/budget           — 查询预算列表（按年月筛选）
 *   POST /api/budget           — 保存预算（新增或更新，INSERT ON DUPLICATE KEY UPDATE）
 *   GET  /api/budget/progress  — 查询预算进度（含已支出金额、百分比、超支标记）
 *   GET  /api/budget/alert     — 查询预算预警（仅超支项）
 *
 * 被前端调用：→ api/budget.js 的 getBudgetList/saveBudget/getBudgetProgress/getBudgetAlert
 * 被 BudgetPage.vue 调用
 *
 * 定时预警：→ BudgetScheduler 每日 2:00 自动检查日/月阈值（详见 scheduler/BudgetScheduler.java）
 */
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

  /** → BudgetService：处理预算 CRUD + 进度计算 + 预警的业务逻辑 */
  private final BudgetService budgetService;

  /**
   * 查询预算列表接口
   *
   * 流程：按 userId + year/month 筛选 → 关联 category 表获取分类名
   *     → 返回预算列表（含分类名、金额、月份）
   *
   * @param year    年份筛选（可选，格式 yyyy）
   * @param month   月份筛选（可选，格式 MM）
   * @param request HTTP 请求
   * @return Result<List<BudgetDTO>> 预算列表
   *
   * 被前端 BudgetPage.vue 列表 + 筛选调用
   */
  @GetMapping
  public Result<List<BudgetDTO>> list(
      @RequestParam(required = false) String year,
      @RequestParam(required = false) String month,
      HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → BudgetService.list()：查询预算表 + JOIN category 获取分类名
    List<BudgetDTO> list = budgetService.list(userId, year, month);
    return Result.success(list);
  }

  /**
   * 保存预算接口（新增或更新）
   *
   * 流程：@Valid 校验分类+月份+金额 → 校验分类类型必须为「支出」（收入不做预算）
   *     → INSERT ON DUPLICATE KEY UPDATE（同一用户+分类+月份自动更新）
   *
   * @param request 预算请求体（categoryId + month + amount，含 @Valid 校验）
   * @param httpRequest HTTP 请求
   * @return Result<BudgetDTO> 保存后的预算信息
   *
   * 被前端 BudgetPage.vue 新增/编辑预算弹窗调用
   * month 格式：yyyy-MM（如 2026-05）
   * 业务异常码：1009 = 收入分类不能设置预算
   */
  @PostMapping
  public Result<BudgetDTO> save(@Valid @RequestBody BudgetRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    // → BudgetService.save()：
    //   1. 校验分类存在且类型为支出(type=2)
    //   2. INSERT ON DUPLICATE KEY UPDATE（同一 user+category+month 唯一键）
    BudgetDTO budget = budgetService.save(userId, request);
    return Result.success(budget, "预算保存成功");
  }

  /**
   * 查询预算进度接口
   *
   * 流程：查询预算列表 → 逐个查询该分类本月已支出金额
   *     → 计算百分比 = (已支出/预算金额) × 100% → 判断是否超支
   *
   * @param year    年份筛选（可选）
   * @param month   月份筛选（可选）
   * @param request HTTP 请求
   * @return Result<List<BudgetProgressDTO>> 预算进度（含已支出金额、百分比、超支标记）
   *
   * 被前端 BudgetPage.vue 进度条展示调用
   * 进度颜色：<80% 绿色 / 80-100% 黄色 / >100% 红色
   */
  @GetMapping("/progress")
  public Result<List<BudgetProgressDTO>> getProgress(
      @RequestParam(required = false) String year,
      @RequestParam(required = false) String month,
      HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → BudgetService.getProgress()：查询预算 → 逐个查已支出 → 计算百分比 + 超支标记
    List<BudgetProgressDTO> list = budgetService.getProgress(userId, year, month);
    return Result.success(list);
  }

  /**
   * 查询预算预警接口（仅超支项）
   *
   * 流程：与 getProgress 相同 → 过滤仅保留 overspent=true 的项
   *
   * @param year    年份筛选（可选）
   * @param month   月份筛选（可选）
   * @param request HTTP 请求
   * @return Result<List<BudgetProgressDTO>> 仅超支的预算项
   *
   * 被前端 BudgetPage.vue 预警面板调用
   * 另外 → BudgetScheduler 定时任务也会触发类似的预警检查
   */
  @GetMapping("/alert")
  public Result<List<BudgetProgressDTO>> getAlert(
      @RequestParam(required = false) String year,
      @RequestParam(required = false) String month,
      HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → BudgetService.getAlert()：复用 getProgress 逻辑 → 过滤 overspent=true
    List<BudgetProgressDTO> list = budgetService.getAlert(userId, year, month);
    return Result.success(list);
  }
}
