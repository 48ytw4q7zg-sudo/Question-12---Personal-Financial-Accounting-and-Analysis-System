package com.example.finance.controller;

import com.example.finance.common.Result;
import com.example.finance.entity.dto.CategoryDTO;
import com.example.finance.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  /**
   * 查询所有分类
   */
  @GetMapping
  public Result<List<CategoryDTO>> list() {
    List<CategoryDTO> list = categoryService.list();
    return Result.success(list);
  }
}
