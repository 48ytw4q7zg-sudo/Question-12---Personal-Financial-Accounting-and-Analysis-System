package com.example.finance.service.impl;

import com.example.finance.common.BusinessException;
import com.example.finance.entity.Account;
import com.example.finance.entity.Transaction;
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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P2-3 CSV import boundary tests — file size, format, record count validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CSV导入边界值测试")
class CsvImportBoundaryTest {

  @Mock TransactionMapper transactionMapper;
  @Mock AccountMapper accountMapper;
  @Mock CategoryMapper categoryMapper;
  @InjectMocks TransactionServiceImpl transactionService;

  Account acct;

  @BeforeEach void setUp() {
    acct = new Account(); acct.setId(1L); acct.setUserId(1L);
    acct.setName("测试账户"); acct.setInitialBalance(new BigDecimal("1000.00")); acct.setStatus(1);
  }

  @Test @DisplayName("CSV: 文件超过5MB → 拒绝[3001]")
  void importCsv_fileTooLarge_rejected() {
    when(accountMapper.selectById(1L)).thenReturn(acct);
    byte[] bigContent = new byte[6 * 1024 * 1024]; // 6MB > 5MB limit
    MultipartFile file = new MockMultipartFile("test.csv", "test.csv", "text/csv", bigContent);
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.importCsv(1L, file, 1L));
    assertEquals(3001, ex.getCode());
    assertTrue(ex.getMessage().contains("5MB"));
  }

  @Test @DisplayName("CSV: 非.csv扩展名 → 拒绝[3001]")
  void importCsv_nonCsvExtension_rejected() {
    when(accountMapper.selectById(1L)).thenReturn(acct);
    MultipartFile file = new MockMultipartFile("data.txt", "data.txt", "text/plain", "a,b,c".getBytes());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.importCsv(1L, file, 1L));
    assertEquals(3001, ex.getCode());
    assertTrue(ex.getMessage().contains(".csv"));
  }

  @Test @DisplayName("CSV: 空文件名 → 拒绝[3001]")
  void importCsv_nullFilename_rejected() {
    when(accountMapper.selectById(1L)).thenReturn(acct);
    MultipartFile file = new MockMultipartFile("file", null, "text/csv", "a".getBytes());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.importCsv(1L, file, 1L));
    assertEquals(3001, ex.getCode());
  }

  @Test @DisplayName("CSV: 有效CSV成功导入")
  void importCsv_validCsv_success() throws Exception {
    when(accountMapper.selectById(1L)).thenReturn(acct);
    String csv = "time,categoryId,type,amount,note\n2026-05-18 12:00:00,1,2,50.00,午餐\n2026-05-18 18:00:00,3,2,120.00,购物\n";
    MockMultipartFile file = new MockMultipartFile("data.csv", "data.csv", "text/csv",
        csv.getBytes(StandardCharsets.UTF_8));
    when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);

    String result = transactionService.importCsv(1L, file, 1L);
    assertTrue(result.contains("成功 2 条"));
    assertTrue(result.contains("失败 0 条"));
    verify(transactionMapper, times(2)).insert(any(Transaction.class));
  }

  @Test @DisplayName("CSV: 混有无效行→部分成功")
  void importCsv_mixedRows_partialSuccess() throws Exception {
    when(accountMapper.selectById(1L)).thenReturn(acct);
    String csv = "time,categoryId,type,amount,note\n2026-05-18 12:00:00,1,2,50.00,ok\ninvalid,row,data\n";
    MockMultipartFile file = new MockMultipartFile("data.csv", "data.csv", "text/csv",
        csv.getBytes(StandardCharsets.UTF_8));
    when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);

    String result = transactionService.importCsv(1L, file, 1L);
    assertTrue(result.contains("成功 1 条"));
    assertTrue(result.contains("失败 1 条"));
  }

  @Test @DisplayName("CSV: 超过1000条记录→中途拒绝")
  void importCsv_exceedsMaxRecords() {
    when(accountMapper.selectById(1L)).thenReturn(acct);
    StringBuilder csv = new StringBuilder("time,categoryId,type,amount,note\n");
    for (int i = 0; i < 1001; i++) csv.append("2026-05-18 12:00:00,1,2,10.00,t\n");
    MockMultipartFile file = new MockMultipartFile("data.csv", "data.csv", "text/csv",
        csv.toString().getBytes(StandardCharsets.UTF_8));
    // Return 1 for every insert call (default applies to all)
    when(transactionMapper.insert(any(Transaction.class))).thenReturn(1);

    assertThrows(BusinessException.class, () -> transactionService.importCsv(1L, file, 1L));
  }

  @Test @DisplayName("CSV: 账户不存在→拒绝[3002]")
  void importCsv_accountNotFound_rejected() {
    when(accountMapper.selectById(999L)).thenReturn(null);
    MultipartFile file = new MockMultipartFile("data.csv", "data.csv", "text/csv", "a".getBytes());
    BusinessException ex = assertThrows(BusinessException.class,
        () -> transactionService.importCsv(1L, file, 999L));
    assertEquals(3002, ex.getCode());
  }
}
