package com.example.finance.entity.dto;

import lombok.Data;

/**
 * 分类数据传输对象
 */
@Data
public class CategoryDTO {

  private Long id;
  private String name;
  private Integer type;
}
