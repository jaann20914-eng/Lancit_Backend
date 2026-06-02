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

// 캘린더 카테고리 CRUD - 프리랜서(USER) / 회사(COMPANY) 공용
// SecurityUtil 로 이메일 + role 꺼내서 ownerType 분기
@RestController
@RequestMapping("/api/calendar/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // CAL-01 / CLI-CAL-01 카테고리 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategories() {
        // TODO 영은 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: OwnerType ownerType = "USER".equals(SecurityUtil.getCurrentRole()) ? OwnerType.USER : OwnerType.COMPANY
        // TODO 영은 [3]: return ResponseEntity.ok(ApiResponse.ok(categoryService.getAll(email, ownerType)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-01 / CLI-CAL-01 카테고리 추가
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCategory(@RequestBody CategoryDTO dto) {
        // TODO 영은 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: OwnerType ownerType = "USER".equals(SecurityUtil.getCurrentRole()) ? OwnerType.USER : OwnerType.COMPANY
        // TODO 영은 [3]: categoryService.create(dto, email, ownerType)
        // TODO 영은 [4]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-02 / CLI-CAL-02 카테고리 수정 (@OwnerCheck 로 소유자 검증)
    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(@PathVariable int categoryId,
                                                             @RequestBody CategoryDTO dto) {
        // TODO 영은 [1]: categoryService.update(categoryId, dto)
        // TODO 영은 [2]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-03 / CLI-CAL-03 카테고리 삭제 (@OwnerCheck 로 소유자 검증)
    // RESTRICT FK → 연관 Task 남아있으면 DB 에러 → 서비스에서 Task 먼저 삭제 처리
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable int categoryId) {
        // TODO 영은 [1]: categoryService.delete(categoryId)
        // TODO 영은 [2]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}