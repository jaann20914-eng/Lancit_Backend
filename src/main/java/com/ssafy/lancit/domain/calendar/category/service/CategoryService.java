package com.ssafy.lancit.domain.calendar.category.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.global.enums.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

// 캘린더 카테고리 CRUD
// 1차 구현은 Controller 에서 전달한 USER ownerType 기준으로 처리
@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final CategoryMapper categoryMapper;

    // CAL-01 / CLI-CAL-01 카테고리 목록 조회
    public List<CategoryDTO> getAll(String email, OwnerType ownerType) {
        return categoryMapper.findByOwner(email, ownerType);
    }

    // CAL-01 카테고리 추가
    @Transactional
    public void create(CategoryDTO dto, String email, OwnerType ownerType) {
        validateCategory(dto);

        dto.setEmail(email);
        dto.setOwnerType(ownerType);

        int insertedCount = categoryMapper.insert(dto);
        if (insertedCount == 0) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // CAL-02 카테고리 수정
    @Transactional
    public void update(int categoryId, CategoryDTO dto, String email, OwnerType ownerType) {
        validateCategory(dto);
        validateCategoryOwner(categoryId, email, ownerType);

        dto.setCategoryId(categoryId);
        dto.setEmail(email);
        dto.setOwnerType(ownerType);

        int updatedCount = categoryMapper.update(dto);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    // CAL-03 카테고리 삭제
    // TODO 2일차: 삭제 전 기존 일정을 다른 카테고리로 이동시키는 정책 완성
    @Transactional
    public void delete(int categoryId, String email, OwnerType ownerType) {
        validateCategoryOwner(categoryId, email, ownerType);

        int taskCount = categoryMapper.countTasksByCategoryIdAndOwner(categoryId, email, ownerType);
        if (taskCount > 0) {
            throw new CustomException(ErrorCode.CATEGORY_HAS_TASKS);
        }

        int deletedCount = categoryMapper.deleteByIdAndOwner(categoryId, email, ownerType);
        if (deletedCount == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    private void validateCategoryOwner(int categoryId, String email, OwnerType ownerType) {
        if (categoryMapper.findByIdAndOwner(categoryId, email, ownerType) == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    private void validateCategory(CategoryDTO dto) {
        if (dto == null
                || !StringUtils.hasText(dto.getCategoryName())
                || !StringUtils.hasText(dto.getColor())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (!HEX_COLOR_PATTERN.matcher(dto.getColor()).matches()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
