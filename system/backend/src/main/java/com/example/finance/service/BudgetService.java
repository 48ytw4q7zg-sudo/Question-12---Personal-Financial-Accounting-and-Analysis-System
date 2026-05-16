package com.example.finance.service;

import com.example.finance.entity.dto.BudgetDTO;
import com.example.finance.entity.dto.BudgetProgressDTO;
import com.example.finance.entity.dto.BudgetRequest;

import java.util.List;

/**
 * 预算服务接口
 */
public interface BudgetService {

  /**
   * 查询用户预算列表
   */
  List<BudgetDTO> list(Long userId, String year, String month);

  /**
   * 保存预算（INSERT ON DUPLICATE KEY UPDATE）
   */
  BudgetDTO save(Long userId, BudgetRequest request);

  /**
   * 获取预算进度
   */
  List<BudgetProgressDTO> getProgress(Long userId, String year, String month);

  /**
   * 获取预算预警（仅返回超支项）
   */
  List<BudgetProgressDTO> getAlert(Long userId, String year, String month);
}
