package com.ssafy.lancit.domain.calendar.category.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.domain.calendar.category.service.CategoryService;
import com.ssafy.lancit.global.enums.OwnerType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 캘린더 카테고리 CRUD - 프리랜서(user) / 회사(company) 공용
// SecurityUtil 로 이메일 + role 꺼내서 ownerType 분기
@RestController
@RequestMapping("/api/calendar/categories")
@RequiredArgsConstructor
@Tag(name = "Calendar Category", description = "캘린더 카테고리 API")
public class CategoryController {

    private final CategoryService categoryService;

    // CAL-01 / CLI-CAL-01 카테고리 목록 조회
    @GetMapping
    @Operation(summary = "카테고리 목록 조회")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategories() {
        String email = SecurityUtil.getCurrentEmail();
        OwnerType ownerType = getCurrentOwnerType();
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAll(email, ownerType)));
    }

    // CAL-01 / CLI-CAL-01 카테고리 추가
    @PostMapping
    @Operation(summary = "카테고리 등록")
    public ResponseEntity<ApiResponse<Void>> createCategory(@RequestBody CategoryDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        OwnerType ownerType = getCurrentOwnerType();
        categoryService.create(dto, email, ownerType);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-02 / CLI-CAL-02 카테고리 수정 (서비스 내부에서 소유자 검증)
    @PutMapping("/{categoryId}")
    @Operation(summary = "카테고리 수정")
    public ResponseEntity<ApiResponse<Void>> updateCategory(@PathVariable int categoryId,
                                                             @RequestBody CategoryDTO dto) {
        categoryService.update(categoryId, dto);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-03 / CLI-CAL-03 카테고리 삭제 (서비스 내부에서 소유자 검증)
    // RESTRICT FK → 연관 Task 의 categoryId 를 먼저 이동한 뒤 카테고리 삭제
    @DeleteMapping("/{categoryId}")
    @Operation(summary = "카테고리 삭제", description = "연결된 일정이 있으면 moveToCategoryId 카테고리로 이동한 뒤 카테고리만 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable int categoryId,
                                                            @RequestParam(required = false) Integer moveToCategoryId) {
        categoryService.deleteCategoryWithTaskMove(categoryId, moveToCategoryId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private OwnerType getCurrentOwnerType() {
        return OwnerType.fromRole(SecurityUtil.getCurrentRole());
    }
}
