package com.ssafy.lancit.domain.calendar.category.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {
 
    private final CategoryMapper categoryMapper;
    private final TaskMapper taskMapper;
 
    /** CAL-01 / CLI-CAL-01 카테고리 목록 조회 */
    public List<CategoryDTO> getAll() {
        // TODO 영은: email + ownerType 추출 → categoryMapper.findAll(email, ownerType)
        return null;
    }
 
    /** CAL-01 카테고리 추가 */
    @Transactional
    public void create(CategoryDTO dto) {
        // TODO 영은: email + ownerType 세팅 → categoryMapper.insert(dto)
    }
 
    /** CAL-02 카테고리 수정 */
    @Transactional
    public void update(int categoryId, CategoryDTO dto) {
        // TODO 영은: categoryMapper.update(categoryId, dto)
    }
 
    /** CAL-03 카테고리 삭제
     *  - 연관 Task 먼저 처리 (이동 or 삭제) → RESTRICT FK 주의 */
    @Transactional
    public void delete(int categoryId) {
        // TODO 영은: taskMapper.countByCategory(categoryId) > 0 이면 예외 or 이동 처리
        //   categoryMapper.delete(categoryId)
    }
}