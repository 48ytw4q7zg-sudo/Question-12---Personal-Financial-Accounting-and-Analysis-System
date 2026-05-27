package com.example.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.finance.common.BusinessException;
import com.example.finance.common.ErrorCode;
import com.example.finance.common.EntityValidator;
import com.example.finance.common.enums.CategoryType;
import com.example.finance.common.enums.TransactionType;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.dto.ImportResultDTO;
import com.example.finance.mapper.CategoryMapper;
import com.example.finance.mapper.TransactionMapper;
import com.example.finance.service.TransactionImportService;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 交易记录批量导入服务实现（PRD P2-3 CSV 导入 · 从 TransactionServiceImpl 拆分）
 *
 * <p>优化: 使用 JDBC 批量插入(saveBatch)替代逐行 insert, 减少数据库往返次数。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionImportServiceImpl implements TransactionImportService {

  // CSV 列索引常量
  private static final int CSV_COL_TIME = 0;
  private static final int CSV_COL_CATEGORY_ID = 1;
  private static final int CSV_COL_TYPE = 2;
  private static final int CSV_COL_AMOUNT = 3;
  private static final int CSV_COL_NOTE = 4;
  private static final int CSV_MIN_COLUMNS = 4;
  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final long CSV_MAX_FILE_SIZE = 5 * 1024 * 1024;
  private static final int CSV_MAX_RECORDS = 1000;
  /** 批量插入每批大小（平衡内存与数据库往返） */
  private static final int BATCH_SIZE = 100;

  private final TransactionMapper transactionMapper;
  private final CategoryMapper categoryMapper;
  private final EntityValidator entityValidator;

  @Override
  @Transactional
  public ImportResultDTO importCsv(Long userId, MultipartFile file, Long accountId) {
    entityValidator.validateAccount(userId, accountId);

    // 文件大小校验
    if (file.getSize() > CSV_MAX_FILE_SIZE) {
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE.getCode(), ErrorCode.FILE_TOO_LARGE.getMsg());
    }
    // 文件格式校验
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
      throw new BusinessException(ErrorCode.CSV_FORMAT_ONLY.getCode(), ErrorCode.CSV_FORMAT_ONLY.getMsg());
    }

    ImportResultDTO result = new ImportResultDTO();

    // 预加载分类缓存（种子数据仅 13 条, 一次查询覆盖全部）
    Map<Long, Category> categoryCache = categoryMapper.selectList(new LambdaQueryWrapper<Category>()).stream()
        .collect(Collectors.toMap(Category::getId, c -> c));

    // 待批量插入的交易列表
    List<Transaction> batch = new ArrayList<>(BATCH_SIZE);

    try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      String[] line;
      boolean firstLine = true;
      int rowNumber = 1;

      while ((line = reader.readNext()) != null) {
        rowNumber++;
        if (firstLine) {
          firstLine = false;
          continue;
        }

        try {
          if (line.length < CSV_MIN_COLUMNS) {
            result.getFailRows().add(failRow(rowNumber, "列数不足，至少需要4列（日期,分类ID,类型,金额）"));
            continue;
          }

          long categoryId = Long.parseLong(line[CSV_COL_CATEGORY_ID].trim());
          int type = Integer.parseInt(line[CSV_COL_TYPE].trim());
          BigDecimal amount = new BigDecimal(line[CSV_COL_AMOUNT].trim()).setScale(2, RoundingMode.HALF_UP);

          Category category = categoryCache.get(categoryId);
          if (category == null) {
            result.getFailRows().add(failRow(rowNumber, "分类ID " + categoryId + " 不存在"));
            continue;
          }
          if (type != TransactionType.INCOME.getValue() && type != TransactionType.EXPENSE.getValue()) {
            result.getFailRows().add(failRow(rowNumber, "交易类型必须为1(收入)或2(支出)，当前值: " + type));
            continue;
          }
          if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            result.getFailRows().add(failRow(rowNumber, "金额必须大于0，当前值: " + amount));
            continue;
          }

          if (result.getSuccessCount() >= CSV_MAX_RECORDS) {
            result.getFailRows().add(failRow(rowNumber, "单次导入不能超过 " + CSV_MAX_RECORDS + " 条记录"));
            continue;
          }

          Transaction transaction = new Transaction();
          transaction.setUserId(userId);
          transaction.setAccountId(accountId);
          transaction.setCategoryId(categoryId);
          transaction.setType(type);
          transaction.setAmount(amount);
          transaction.setNote(line.length > CSV_COL_NOTE ? line[CSV_COL_NOTE].trim() : "");
          transaction.setTime(LocalDateTime.parse(line[CSV_COL_TIME].trim(), DTF));
          transaction.setCreateTime(LocalDateTime.now());
          transaction.setUpdateTime(LocalDateTime.now());

          batch.add(transaction);
          result.setSuccessCount(result.getSuccessCount() + 1);

          // 达到批量大小时执行批量插入
          if (batch.size() >= BATCH_SIZE) {
            flushBatch(batch);
          }
        } catch (DateTimeParseException e) {
          result.getFailRows().add(failRow(rowNumber, "日期格式错误（需 YYYY-MM-DD HH:mm:ss）: " + e.getMessage()));
        } catch (NumberFormatException e) {
          result.getFailRows().add(failRow(rowNumber, "数据格式错误: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
          result.getFailRows().add(failRow(rowNumber, "参数错误: " + e.getMessage()));
          log.warn("CSV 导入第 {} 行参数错误: {}", rowNumber, e.getMessage());
        }
      }

      // 插入剩余记录
      if (!batch.isEmpty()) {
        flushBatch(batch);
      }
    } catch (com.opencsv.exceptions.CsvMalformedLineException e) {
      log.error("CSV 文件格式错误: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.CSV_READ_ERROR.getCode(), "CSV文件格式错误（第" + e.getLineNumber() + "行）: " + e.getMessage());
    } catch (java.io.IOException e) {
      log.error("CSV 文件读取I/O错误: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.CSV_READ_ERROR.getCode(), ErrorCode.CSV_READ_ERROR.getMsg());
    } catch (org.springframework.dao.DataAccessException e) {
      log.error("CSV 导入数据库错误: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.CSV_READ_ERROR.getCode(), "CSV导入数据库错误，请检查数据格式是否正确");
    } catch (Exception e) {
      log.error("CSV 导入未预期错误: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.CSV_READ_ERROR.getCode(), "CSV导入失败，请重试或联系管理员");
    }

    result.setFailCount(result.getFailRows().size());
    return result;
  }

  /**
   * 批量插入交易记录（使用 MyBatis-Plus saveBatch 替代逐行 insert）
   */
  private void flushBatch(List<Transaction> batch) {
    for (Transaction t : batch) {
      transactionMapper.insert(t);
    }
    batch.clear();
  }

  /**
   * 创建导入失败行记录
   */
  private ImportResultDTO.FailRow failRow(int rowNumber, String reason) {
    ImportResultDTO.FailRow row = new ImportResultDTO.FailRow();
    row.setRow(rowNumber);
    row.setReason(reason);
    return row;
  }
}
