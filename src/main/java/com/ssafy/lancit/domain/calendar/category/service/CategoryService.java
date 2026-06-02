package com.ssafy.lancit.domain.calendar.category.service;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.global.enums.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 캘린더 카테고리 CRUD - 프리랜서(USER) / 회사(COMPANY) 공용
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final TaskMapper taskMapper;

    // CAL-01 / CLI-CAL-01 카테고리 목록 조회
    public List<CategoryDTO> getAll(String email, OwnerType ownerType) {
        // TODO 영은 [1]: return categoryMapper.findAll(email, ownerType)
        return null;
    }

    // CAL-01 카테고리 추가
    @Transactional
    public void create(CategoryDTO dto, String email, OwnerType ownerType) {
        // TODO 영은 [1]: dto.setEmail(email)
        // TODO 영은 [2]: dto.setOwnerType(ownerType)
        // TODO 영은 [3]: categoryMapper.insert(dto)
    }

    // CAL-02 카테고리 수정 (@OwnerCheck 로 소유자 검증)
    @OwnerCheck(resourceType = "CATEGORY")
    @Transactional
    public void update(int categoryId, CategoryDTO dto) {
        // TODO 영은 [1]: categoryMapper.update(categoryId, dto)
    }

    // CAL-03 카테고리 삭제 (@OwnerCheck 로 소유자 검증)
    // RESTRICT FK → 연관 Task 있으면 DB 에러 발생
    // 정석: Task 존재 여부 먼저 확인 → 있으면 예외 → 프론트가 Task 먼저 처리 후 재요청
    @OwnerCheck(resourceType = "CATEGORY")
    @Transactional
    public void delete(int categoryId) {
        // TODO 영은 [1]: int count = taskMapper.countByCategory(categoryId)
        //               count > 0 이면 throw new CustomException(ErrorCode.CATEGORY_HAS_TASKS)
        //               → 프론트에서 "이 카테고리에 일정이 있습니다. 먼저 삭제하거나 이동해주세요." 안내
        // TODO 영은 [2]: categoryMapper.delete(categoryId)
    }
}