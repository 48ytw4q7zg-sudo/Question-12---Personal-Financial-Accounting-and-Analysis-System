package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.RecurringBillDTO;
import com.example.finance.entity.dto.RecurringBillRequest;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.RecurringBillService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 周期性账单控制器
 */
@RestController
@RequestMapping("/api/recurring-bill")
@RequiredArgsConstructor
public class RecurringBillController {

  private final RecurringBillService recurringBillService;

  /**
   * 查询周期性账单列表
   */
  @GetMapping
  public Result<List<RecurringBillDTO>> list(HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    List<RecurringBillDTO> list = recurringBillService.list(userId);
    return Result.success(list);
  }

  /**
   * 创建周期性账单
   */
  @PostMapping
  public Result<RecurringBillDTO> create(@Valid @RequestBody RecurringBillRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    RecurringBillDTO bill = recurringBillService.create(userId, request);
    return Result.success(bill, "周期性账单创建成功");
  }

  /**
   * 更新周期性账单
   */
  @PutMapping("/{id}")
  public Result<RecurringBillDTO> update(@PathVariable Long id,
      @Valid @RequestBody RecurringBillRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    RecurringBillDTO bill = recurringBillService.update(userId, id, request);
    return Result.success(bill, "周期性账单更新成功");
  }

  /**
   * 停用周期性账单
   */
  @DeleteMapping("/{id}")
  public Result<Void> deactivate(@PathVariable Long id, HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    recurringBillService.deactivate(userId, id);
    return Result.success(null, "周期性账单已停用");
  }

  /**
   * 生成交易记录
   */
  @PostMapping("/{id}/generate")
  public Result<TransactionDTO> generate(@PathVariable Long id, HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    TransactionDTO transaction = recurringBillService.generate(userId, id);
    return Result.success(transaction, "交易记录已生成");
  }
}
