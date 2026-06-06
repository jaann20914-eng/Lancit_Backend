package com.ssafy.lancit.domain.calendar.category.service;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.global.enums.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

// 캘린더 카테고리 CRUD - 프리랜서(USER) / 회사(COMPANY) 공용
@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private final CategoryMapper categoryMapper;
    private final TaskMapper taskMapper;

    // CAL-01 / CLI-CAL-01 카테고리 목록 조회
    public List<CategoryDTO> getAll(String email, OwnerType ownerType) {
        return categoryMapper.findAll(email, ownerType);
    }

    // CAL-01 카테고리 추가
    @Transactional
    public void create(CategoryDTO dto, String email, OwnerType ownerType) {
        validateCreate(dto);

        dto.setEmail(email);
        dto.setOwnerType(ownerType);
        categoryMapper.insert(dto);
    }

    // CAL-02 카테고리 수정 (@OwnerCheck 로 소유자 검증)
    @OwnerCheck(resourceType = "CATEGORY")
    @Transactional
    public void update(int categoryId, CategoryDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        OwnerType ownerType = getCurrentOwnerType();

        if (!categoryMapper.existsByIdAndOwner(categoryId, email, ownerType)) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        validateUpdate(dto);
        categoryMapper.update(categoryId, dto);
    }

    // CAL-03 카테고리 삭제 (@OwnerCheck 로 삭제 대상 소유자 검증)
    // RESTRICT FK → 연관 Task 의 categoryId 를 먼저 이동한 뒤 카테고리 삭제
    @OwnerCheck(resourceType = "CATEGORY")
    @Transactional
    public void deleteCategoryWithTaskMove(int categoryId, Integer moveToCategoryId) {
        String email = SecurityUtil.getCurrentEmail();
        OwnerType ownerType = getCurrentOwnerType();

        if (!categoryMapper.existsByIdAndOwner(categoryId, email, ownerType)) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        int linkedTaskCount = taskMapper.countByCategory(categoryId);
        if (linkedTaskCount > 0 && moveToCategoryId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (moveToCategoryId != null) {
            if (categoryId == moveToCategoryId) {
                throw new CustomException(ErrorCode.INVALID_CATEGORY_MOVE);
            }
            if (!categoryMapper.existsByIdAndOwner(moveToCategoryId, email, ownerType)) {
                throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
            }
        }

        if (linkedTaskCount > 0) {
            taskMapper.updateCategoryByCategory(categoryId, moveToCategoryId, email, ownerType);
        }

        int deletedCount = categoryMapper.deleteByIdAndOwner(categoryId, email, ownerType);
        if (deletedCount == 0) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    private OwnerType getCurrentOwnerType() {
        return "USER".equals(SecurityUtil.getCurrentRole()) ? OwnerType.USER : OwnerType.COMPANY;
    }

    private void validateCreate(CategoryDTO dto) {
        if (dto == null || isBlank(dto.getCategoryName()) || isBlank(dto.getColor())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        validateColor(dto.getColor());
    }

    private void validateUpdate(CategoryDTO dto) {
        if (dto == null || (dto.getCategoryName() == null && dto.getColor() == null)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getCategoryName() != null && isBlank(dto.getCategoryName())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getColor() != null) {
            if (isBlank(dto.getColor())) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            validateColor(dto.getColor());
        }
    }

    private void validateColor(String color) {
        if (!HEX_COLOR_PATTERN.matcher(color).matches()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
