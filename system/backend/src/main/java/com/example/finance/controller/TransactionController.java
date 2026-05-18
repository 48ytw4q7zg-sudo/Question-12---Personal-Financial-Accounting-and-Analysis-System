package com.example.finance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.finance.common.Result;
import com.example.finance.entity.dto.TransactionDTO;
import com.example.finance.entity.dto.TransactionRequest;
import com.example.finance.entity.dto.TransferDTO;
import com.example.finance.entity.dto.TransferRequest;
import com.example.finance.interceptor.LoginInterceptor;
import com.example.finance.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 交易记录控制器
 */
@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionService transactionService;

  /**
   * 查询交易记录（分页 + 筛选）
   */
  @GetMapping
  public Result<IPage<TransactionDTO>> list(
      @RequestParam(required = false) Long accountId,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false, defaultValue = "time") String sortBy,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize,
      HttpServletRequest request) {
    Long userId = LoginInterceptor.getUserId(request);
    IPage<TransactionDTO> page = transactionService.list(
        userId, accountId, categoryId, startTime, endTime, keyword, sortBy, pageNum, pageSize);
    return Result.success(page);
  }

  /**
   * 创建交易记录
   */
  @PostMapping
  public Result<TransactionDTO> create(@Valid @RequestBody TransactionRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    TransactionDTO transaction = transactionService.create(userId, request);
    return Result.success(transaction, "记录创建成功");
  }

  /**
   * 更新交易记录
   */
  @PutMapping("/{id}")
  public Result<TransactionDTO> update(@PathVariable Long id,
      @Valid @RequestBody TransactionRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    TransactionDTO transaction = transactionService.update(userId, id, request);
    return Result.success(transaction, "记录更新成功");
  }

  /**
   * 转账
   */
  @PostMapping("/transfer")
  public Result<TransferDTO> transfer(@Valid @RequestBody TransferRequest request,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    TransferDTO transfer = transactionService.transfer(userId, request);
    return Result.success(transfer, "转账成功");
  }

  /**
   * 导入 CSV
   */
  @PostMapping("/import")
  public Result<String> importCsv(@RequestParam("file") MultipartFile file,
      @RequestParam("accountId") Long accountId,
      HttpServletRequest httpRequest) {
    Long userId = (Long) httpRequest.getAttribute("userId");
    String result = transactionService.importCsv(userId, file, accountId);
    return Result.success(result);
  }
}
