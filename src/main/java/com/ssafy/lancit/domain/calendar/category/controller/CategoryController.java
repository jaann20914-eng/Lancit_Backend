package com.ssafy.lancit.domain.calendar.category.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.domain.calendar.category.service.CategoryService;
import com.ssafy.lancit.global.enums.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 캘린더 카테고리 CRUD
// 1차 구현은 로그인한 USER 기준으로 처리하고, COMPANY 캘린더는 추후 확장 예정
@RestController
@RequestMapping("/api/calendar/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final OwnerType DAY_ONE_OWNER_TYPE = OwnerType.USER;

    private final CategoryService categoryService;

    // CAL-01 / CLI-CAL-01 카테고리 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategories() {
        String email = SecurityUtil.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAll(email, DAY_ONE_OWNER_TYPE)));
    }

    // CAL-01 / CLI-CAL-01 카테고리 추가
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCategory(@RequestBody CategoryDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        categoryService.create(dto, email, DAY_ONE_OWNER_TYPE);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-02 / CLI-CAL-02 카테고리 수정
    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(@PathVariable int categoryId,
                                                             @RequestBody CategoryDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        categoryService.update(categoryId, dto, email, DAY_ONE_OWNER_TYPE);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-03 / CLI-CAL-03 카테고리 삭제
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable int categoryId) {
        String email = SecurityUtil.getCurrentEmail();
        categoryService.delete(categoryId, email, DAY_ONE_OWNER_TYPE);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
