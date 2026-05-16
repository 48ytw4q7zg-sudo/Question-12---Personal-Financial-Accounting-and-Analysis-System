package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.BudgetDTO;
import com.example.finance.entity.dto.BudgetProgressDTO;
import com.example.finance.entity.dto.BudgetRequest;
import com.example.finance.service.BudgetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预算控制器
 */
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

  private final BudgetService budgetService;

  /**
   * 查询预算列表
   */
  @GetMapping
  public Result<List<BudgetDTO>> list(@RequestParam String year, @RequestParam String month,
      HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<BudgetDTO> list = budgetService.list(userId, year, month);
    return Result.success(list);
  }

  /**
   * 保存预算
   */
  @PostMapping
  public Result<BudgetDTO> save(@Valid @RequestBody BudgetRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    BudgetDTO budget = budgetService.save(userId, request);
    return Result.success(budget, "预算保存成功");
  }

  /**
   * 获取预算进度
   */
  @GetMapping("/progress")
  public Result<List<BudgetProgressDTO>> getProgress(
      @RequestParam String year, @RequestParam String month,
      HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<BudgetProgressDTO> list = budgetService.getProgress(userId, year, month);
    return Result.success(list);
  }

  /**
   * 获取预算预警（仅超支项）
   */
  @GetMapping("/alert")
  public Result<List<BudgetProgressDTO>> getAlert(
      @RequestParam String year, @RequestParam String month,
      HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<BudgetProgressDTO> list = budgetService.getAlert(userId, year, month);
    return Result.success(list);
  }
}
