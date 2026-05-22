package com.example.finance.service.impl;

import com.example.finance.common.BusinessException;
import com.example.finance.common.EntityValidator;
import com.example.finance.entity.Account;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.dto.ImportResultDTO;
import com.example.finance.mapper.AccountMapper;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.TransactionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P2-3 CSV导入边界值测试 — 文件大小/格式/记录数校验 + 码值对齐 ErrorCode 唯一码值
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CSV导入边界值测试")
class CsvImportBoundaryTest {

  @Mock TransactionMapper transactionMapper;
  @Mock AccountMapper accountMapper;
  @Mock CategoryMapper categoryMapper;
  @Mock EntityValidator entityValidator;
  @InjectMocks TransactionServiceImpl transactionService;

  Account acct;

  @BeforeEach void setUp() {
    acct = new Account(); acct.setId(1L); acct.setUserId(1L);
    acct.setName("测试账户"); acct.setInitialBalance(new BigDecimal("1000.00")); acct.setStatus(1);
  }

  @Test @DisplayName("CSV: 文件超过5MB → 拒绝[3010]")
  void importCsv_fileTooLarge_rejected() {
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
    byte[] bigContent = new byte[6 * 1024 * 1024]; // 6MB > 5MB limit
    MultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv", bigContent);
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.importCsv(1L, file, 1L));
    assertEquals(3010, ex.getCode());
    assertTrue(ex.getMessage().contains("5MB"));
  }

  @Test @DisplayName("CSV: 非.csv扩展名 → 拒绝[3002]")
  void importCsv_nonCsvExtension_rejected() {
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
    MultipartFile file = new MockMultipartFile("data.txt", "data.txt", "text/plain", "a,b,c".getBytes());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.importCsv(1L, file, 1L));
    assertEquals(3002, ex.getCode());
    assertTrue(ex.getMessage().contains(".csv"));
  }

  @Test @DisplayName("CSV: 空文件名 → 拒绝[3002]")
  void importCsv_nullFilename_rejected() {
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
    MultipartFile file = new MockMultipartFile("file", null, "text/csv", "a".getBytes());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.importCsv(1L, file, 1L));
    assertEquals(3002, ex.getCode());
  }

  @Test @DisplayName("CSV: 有效CSV成功导入 → 返回 ImportResultDTO")
  void importCsv_validCsv_success() throws Exception {
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
    Category cat1 = new Category(); cat1.setId(1L); cat1.setName("餐饮"); cat1.setType(2);
    Category cat3 = new Category(); cat3.setId(3L); cat3.setName("购物"); cat3.setType(2);
    when(categoryMapper.selectList(any())).thenReturn(java.util.List.of(cat1, cat3));
    String csv = "time,categoryId,type,amount,note\n2026-05-18 12:00:00,1,2,50.00,午餐\n2026-05-18 18:00:00,3,2,120.00,购物\n";
    MockMultipartFile file = new MockMultipartFile("data.csv", "data.csv", "text/csv",
        csv.getBytes(StandardCharsets.UTF_8));
    when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);

    // importCsv 返回 ImportResultDTO（含 successCount/failCount/failRows）
    ImportResultDTO result = transactionService.importCsv(1L, file, 1L);
    assertEquals(2, result.getSuccessCount());
    assertEquals(0, result.getFailCount());
    verify(transactionMapper, times(2)).insert(any(Transaction.class));
  }

  @Test @DisplayName("CSV: 混有无效行→部分成功")
  void importCsv_mixedRows_partialSuccess() throws Exception {
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
    Category cat1 = new Category(); cat1.setId(1L); cat1.setName("餐饮"); cat1.setType(2);
    when(categoryMapper.selectList(any())).thenReturn(java.util.List.of(cat1));
    String csv = "time,categoryId,type,amount,note\n2026-05-18 12:00:00,1,2,50.00,ok\ninvalid,row,data\n";
    MockMultipartFile file = new MockMultipartFile("data.csv", "data.csv", "text/csv",
        csv.getBytes(StandardCharsets.UTF_8));
    when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);

    ImportResultDTO result = transactionService.importCsv(1L, file, 1L);
    assertEquals(1, result.getSuccessCount());
    assertTrue(result.getFailCount() > 0);
  }

  @Test @DisplayName("CSV: 超过1000条记录→超过部分记录为失败行")
  void importCsv_exceedsMaxRecords() {
    when(entityValidator.validateAccount(1L, 1L)).thenReturn(acct);
    Category cat1 = new Category(); cat1.setId(1L); cat1.setName("餐饮"); cat1.setType(2);
    when(categoryMapper.selectList(any())).thenReturn(java.util.List.of(cat1));
    StringBuilder csv = new StringBuilder("time,categoryId,type,amount,note\n");
    for (int i = 0; i < 1001; i++) csv.append("2026-05-18 12:00:00,1,2,10.00,t\n");
    MockMultipartFile file = new MockMultipartFile("data.csv", "data.csv", "text/csv",
        csv.toString().getBytes(StandardCharsets.UTF_8));
    when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);

    // 超过上限不再抛异常，改为将超限行记入 failRows，成功插入1000条
    ImportResultDTO result = transactionService.importCsv(1L, file, 1L);
    assertEquals(1000, result.getSuccessCount());
    assertTrue(result.getFailCount() > 0);
    assertTrue(result.getFailRows().stream().anyMatch(f -> f.getReason().contains("1000")));
  }

  @Test @DisplayName("CSV: 账户不存在→拒绝[3004]")
  void importCsv_accountNotFound_rejected() {
    when(entityValidator.validateAccount(1L, 999L)).thenThrow(new BusinessException(3004, "账户不存在或已禁用"));
    MultipartFile file = new MockMultipartFile("data.csv", "data.csv", "text/csv", "a".getBytes());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.importCsv(1L, file, 999L));
    assertEquals(3004, ex.getCode());
  }
}