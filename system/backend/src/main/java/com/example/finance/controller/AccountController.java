package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.AccountBalanceDTO;
import com.example.finance.entity.dto.AccountDTO;
import com.example.finance.entity.dto.AccountRequest;
import com.example.finance.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账户控制器
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  /**
   * 查询账户列表
   */
  @GetMapping
  public Result<List<AccountDTO>> list(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<AccountDTO> list = accountService.list(userId);
    return Result.success(list);
  }

  /**
   * 创建账户
   */
  @PostMapping
  public Result<AccountDTO> create(@Valid @RequestBody AccountRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    AccountDTO account = accountService.create(userId, request);
    return Result.success(account, "账户创建成功");
  }

  /**
   * 更新账户
   */
  @PutMapping("/{id}")
  public Result<AccountDTO> update(@PathVariable Long id,
      @Valid @RequestBody AccountRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    AccountDTO account = accountService.update(userId, id, request);
    return Result.success(account, "账户更新成功");
  }

  /**
   * 删除账户（软删除）
   */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    accountService.delete(userId, id);
    return Result.success(null, "账户删除成功");
  }

  /**
   * 获取账户余额统计
   */
  @GetMapping("/balance")
  public Result<List<AccountBalanceDTO>> getBalance(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute("userId");
    List<AccountBalanceDTO> list = accountService.getBalance(userId);
    return Result.success(list);
  }
}
