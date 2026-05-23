package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.AccountBalanceDTO;
import com.example.finance.entity.dto.AccountDTO;
import com.example.finance.entity.dto.AccountRequest;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账户控制器（PRD P0-2 账户 CRUD + P0-5 按账户汇总余额）
 *
 * 职责：接收账户管理的 HTTP 请求，参数校验后转发 AccountService 处理
 * 路由前缀：/api/v1/account
 * 依赖：→ AccountService（业务逻辑层）→ AccountMapper + TransactionMapper（数据访问层）
 *
 * 接口清单：
 *   GET    /api/account          — 查询当前用户的账户列表
 *   POST   /api/account          — 创建新账户
 *   PUT    /api/account/{id}     — 更新账户信息
 *   DELETE /api/v1/account/{id}     — 删除账户（软删除，status 置 0）
 *   GET    /api/account/balance  — 获取各账户余额统计
 *
 * 被前端调用：→ api/account.js 的 getAccountList/create/update/delete/getBalance
 * 被 AccountPage.vue、DashboardPage.vue、TransferPage.vue 等调用
 */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Validated
public class AccountController {

  /** → AccountService：处理账户 CRUD + 余额统计的业务逻辑 */
  private final AccountService accountService;

  /**
   * 查询账户列表接口
   *
   * 流程：从 JWT 提取 userId → 调用 accountService.list(userId)
   *     → 返回该用户所有正常状态的账户信息
   *
   * @param request HTTP 请求（LoginInterceptor 已注入 userId）
   * @return Result<List<AccountDTO>> 账户列表（含名称、类型、余额、币种等）
   *
   * 被前端 AccountPage.vue 列表展示 + TransferPage.vue 账户下拉 + ImportPage.vue 账户选择调用
   */
  @GetMapping
  public Result<List<AccountDTO>> list(HttpServletRequest request) {
    // → LoginInterceptor.getUserId()：从 request 属性提取 JWT 解析的 userId
    Long userId = LoginInterceptor.getUserId(request);
    // → AccountService.list()：查询该用户所有正常账户（status=1）
    List<AccountDTO> list = accountService.list(userId);
    return Result.success(list);
  }

  /**
   * 创建账户接口
   *
   * 流程：@Valid 校验账户名+类型+余额 → 调用 accountService.create()
   *     → 插入 account 表 → 返回新账户信息
   *
   * @param request 账户创建请求体（name + type + initialBalance + currency，含 @Valid 校验）
   * @param httpRequest HTTP 请求（request.getAttribute("userId") 提取当前用户 ID）
   * @return Result<AccountDTO> 新创建的账户信息
   *
   * 被前端 AccountPage.vue 新建账户弹窗调用
   * 账户类型：1=现金 2=银行卡 3=支付宝 4=微信
   */
  @PostMapping
  public Result<AccountDTO> create(@Valid @RequestBody AccountRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → AccountService.create()：校验账户名唯一性 + 插入数据库
    AccountDTO account = accountService.create(userId, request);
    return Result.success(account, "账户创建成功");
  }

  /**
   * 更新账户接口
   *
   * 流程：@Valid 校验 → 校验账户归属权（userId 匹配）
   *     → 调用 accountService.update() → 更新 account 表
   *
   * @param id 账户 ID（URL 路径参数）
   * @param request 账户更新请求体
   * @param httpRequest HTTP 请求
   * @return Result<AccountDTO> 更新后的账户信息
   *
   * 被前端 AccountPage.vue 编辑账户弹窗调用
   */
  @PutMapping("/{id}")
  public Result<AccountDTO> update(@PathVariable @Min(1) Long id,
      @Valid @RequestBody AccountRequest request,
      HttpServletRequest httpRequest) {
    Long userId = LoginInterceptor.getUserId(httpRequest);
    // → AccountService.update()：校验归属权 + 更新数据库
    AccountDTO account = accountService.update(userId, id, request);
    return Result.success(account, "账户更新成功");
  }

  /**
   * 删除账户接口（软删除，status 置 0）
   *
   * 流程：校验账户归属权 → 检查是否有关联交易记录或周期性账单
   *     → 如有关联则拒绝删除 → 否则 status 置 0（软删除）
   *
   * @param id 账户 ID（URL 路径参数）
   * @param request HTTP 请求
   * @return Result<Void> 成功无返回数据
   *
   * 被前端 AccountPage.vue 删除账户确认弹窗调用
   * 业务异常码：2002 = 账户下有收支记录 / 2003 = 账户下有活跃周期性账单 / 2004 = 账户不存在
   */
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable @Min(1) Long id, HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → AccountService.delete()：检查关联交易记录/周期账单 → status 置 0
    accountService.delete(userId, id);
    return Result.success(null, "账户删除成功");
  }

  /**
   * 获取账户余额统计接口（PRD P0-5 各账户汇总余额）
   *
   * 流程：查询所有账户 → 批量查询每个账户的收入/支出总额（消除 N+1 问题）
   *     → 余额 = 初始余额 + 收入 - 支出
   *
   * @param request HTTP 请求
   * @return Result<List<AccountBalanceDTO>> 各账户余额统计（含初始余额、收入、支出、当前余额）
   *
   * 被前端 AccountPage.vue 余额汇总卡片 + DashboardPage.vue 总资产卡片调用
   * 优化：使用 TransactionMapper.selectAccountIncomeBatch/selectAccountExpenseBatch 批量查询
   */
  @GetMapping("/balance")
  public Result<List<AccountBalanceDTO>> getBalance(HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    // → AccountService.getBalance()：批量查询收入/支出 → 汇总余额（消除 N+1）
    List<AccountBalanceDTO> list = accountService.getBalance(userId);
    return Result.success(list);
  }
}
