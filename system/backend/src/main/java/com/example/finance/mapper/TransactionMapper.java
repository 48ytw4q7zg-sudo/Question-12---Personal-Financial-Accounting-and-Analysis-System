package com.example.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.dto.CategorySummaryDTO;
import com.example.finance.entity.dto.MonthlySummaryDTO;
import com.example.finance.entity.dto.MonthlyTrendDTO;
import com.example.finance.entity.dto.TransactionDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;
import java.util.Map;

/**
 * 交易记录 Mapper（含复杂查询）
 */
@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {

  /**
   * 带筛选条件的交易记录查询（JOIN account + category）
   */
  List<TransactionDTO> selectTransactionList(
      @Param("userId") Long userId,
      @Param("accountId") Long accountId,
      @Param("categoryId") Long categoryId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime,
      @Param("keyword") String keyword,
      @Param("sortBy") String sortBy,
      RowBounds rowBounds
  );

  /**
   * 查询交易记录总数（带筛选）
   */
  Long selectTransactionCount(
      @Param("userId") Long userId,
      @Param("accountId") Long accountId,
      @Param("categoryId") Long categoryId,
      @Param("startTime") String startTime,
      @Param("endTime") String endTime,
      @Param("keyword") String keyword
  );

  /**
   * 月度收支汇总
   */
  MonthlySummaryDTO selectMonthlySummary(
      @Param("userId") Long userId,
      @Param("year") int year,
      @Param("month") int month
  );

  /**
   * 年度收支汇总
   */
  MonthlySummaryDTO selectYearlySummary(
      @Param("userId") Long userId,
      @Param("year") int year
  );

  /**
   * 分类汇总（type 可选）
   */
  List<CategorySummaryDTO> selectCategorySummary(
      @Param("userId") Long userId,
      @Param("year") int year,
      @Param("month") int month,
      @Param("type") Integer type
  );

  /**
   * 月度趋势
   */
  List<MonthlyTrendDTO> selectTrend(
      @Param("userId") Long userId,
      @Param("year") int year
  );

  /**
   * 计算账户总收入
   */
  java.math.BigDecimal selectAccountIncome(@Param("userId") Long userId, @Param("accountId") Long accountId);

  /**
   * 计算账户总支出
   */
  java.math.BigDecimal selectAccountExpense(@Param("userId") Long userId, @Param("accountId") Long accountId);

  /**
   * 批量计算账户收入（消除N+1）
   */
  List<Map<String, Object>> selectAccountIncomeBatch(@Param("userId") Long userId, @Param("accountIds") List<Long> accountIds);

  /**
   * 批量计算账户支出（消除N+1）
   */
  List<Map<String, Object>> selectAccountExpenseBatch(@Param("userId") Long userId, @Param("accountIds") List<Long> accountIds);
}
