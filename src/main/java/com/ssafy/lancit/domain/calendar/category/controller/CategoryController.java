package com.ssafy.lancit.domain.calendar.category.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.domain.calendar.category.service.CategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/calendar/categories")
@RequiredArgsConstructor
public class CategoryController {
 
    private final CategoryService categoryService;
 
    /** CAL-01 / CLI-CAL-01 카테고리 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategories() {
        // TODO 영은: categoryService.getAll(email, ownerType)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CAL-01 / CLI-CAL-01 카테고리 추가 */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCategory(@RequestBody CategoryDTO dto) {
        // TODO 영은: categoryService.create(dto, email, ownerType)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CAL-02 / CLI-CAL-02 카테고리 수정 */
    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(@PathVariable int categoryId,
                                                             @RequestBody CategoryDTO dto) {
        // TODO 영은: categoryService.update(categoryId, dto)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CAL-03 / CLI-CAL-03 카테고리 삭제
     *  - 연관 Task 먼저 처리 (다른 카테고리 이동 or 삭제) 후 category 삭제
     *  - RESTRICT FK → Task 남아있으면 DB 에러 발생 */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable int categoryId) {
        // TODO 영은: categoryService.delete(categoryId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}