package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.entity.dto.MonthlyTrendDTO;
import com.example.finance.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计控制器
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

  private final StatisticsService statisticsService;

  /**
   * 月度收支汇总
   */
  @GetMapping("/monthly")
  public Result<MonthlySummaryDTO> getMonthlySummary(
      @RequestParam int year, @RequestParam int month,
      HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    MonthlySummaryDTO summary = statisticsService.getMonthlySummary(userId, year, month);
    return Result.success(summary);
  }

  /**
   * 年度收支汇总
   */
  @GetMapping("/yearly")
  public Result<MonthlySummaryDTO> getYearlySummary(@RequestParam int year,
      HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    MonthlySummaryDTO summary = statisticsService.getYearlySummary(userId, year);
    return Result.success(summary);
  }

  /**
   * 分类汇总（type 可选，为空时返回全部）
   */
  @GetMapping("/category-summary")
  public Result<List<CategorySummaryDTO>> getCategorySummary(
      @RequestParam int year, @RequestParam int month,
      @RequestParam(required = false) Integer type,
      HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<CategorySummaryDTO> list = statisticsService.getCategorySummary(userId, year, month, type);
    return Result.success(list);
  }

  /**
   * 月度趋势
   */
  @GetMapping("/trend")
  public Result<List<MonthlyTrendDTO>> getTrend(@RequestParam int year,
      HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<MonthlyTrendDTO> list = statisticsService.getTrend(userId, year);
    return Result.success(list);
  }
}
