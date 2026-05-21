package com.example.finance.controller;

import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.Result;
import com.example.finance.entity.dto.BudgetAlertDTO;
import com.example.finance.entity.dto.BudgetDTO;
import com.example.finance.entity.dto.BudgetProgressDTO;
import com.example.finance.entity.dto.BudgetRequest;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.BudgetAlertService;
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
 *   GET    /api/budget           — 查询预算列表（按年月筛选）
 *   POST   /api/budget           — 保存预算（新增或更新，INSERT ON DUPLICATE KEY UPDATE）
 *   DELETE /api/budget/{id}      — 删除预算
 *   GET    /api/budget/progress  — 查询预算进度（含已支出金额、百分比、超支标记）
 *   GET    /api/budget/alert     — 查询预算预警（仅超支项）
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

  private final BudgetService budgetService;
  private final BudgetAlertService budgetAlertService;

  /** year/month 参数校验：如果提供则必须为合法数字且在合理范围内 */
  private void validateYearMonth(String year, String month) {
    if (year != null) {
      try {
        int y = Integer.parseInt(year);
        if (y < 2000 || y > 2100) {
          throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "year需在2000-2100之间");
        }
      } catch (NumberFormatException e) {
        throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "year必须为合法数字");
      }
    }
    if (month != null) {
      try {
        int m = Integer.parseInt(month);
        if (m < 1 || m > 12) {
          throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "month需在1-12之间");
        }
      } catch (NumberFormatException e) {
        throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "month必须为合法数字");
      }
    }
  }

  @GetMapping
  public Result<List<BudgetDTO>> list(
      @RequestParam(required = false) String year,
      @RequestParam(required = false) String month,
      HttpServletRequest request) {
    validateYearMonth(year, month);
    Long userId = LoginInterceptor.getUserId(request);
    List<BudgetDTO> list = budgetService.list(userId, year, month);
    return Result.success(list);
  }

  @PostMapping
  public Result<BudgetDTO> save(@Valid @RequestBody BudgetRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    BudgetDTO budget = budgetService.save(userId, request);
    return Result.success(budget, "预算保存成功");
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    budgetService.delete(userId, id);
    return Result.success(null, "预算已删除");
  }

  @GetMapping("/progress")
  public Result<List<BudgetProgressDTO>> getProgress(
      @RequestParam(required = false) String year,
      @RequestParam(required = false) String month,
      HttpServletRequest request) {
    validateYearMonth(year, month);
    Long userId = LoginInterceptor.getUserId(request);
    List<BudgetProgressDTO> list = budgetService.getProgress(userId, year, month);
    return Result.success(list);
  }

  @GetMapping("/alert")
  public Result<List<BudgetAlertDTO>> getAlert(
      @RequestParam(required = false) String year,
      @RequestParam(required = false) String month,
      HttpServletRequest request) {
    validateYearMonth(year, month);
    Long userId = LoginInterceptor.getUserId(request);
    List<BudgetAlertDTO> list = budgetAlertService.getAlerts(userId, year, month);
    return Result.success(list);
  }
}