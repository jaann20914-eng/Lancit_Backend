package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.global.enums.OwnerType;
import com.ssafy.lancit.global.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

// 캘린더 일정 CRUD
// 1차 구현은 Controller 에서 전달한 USER ownerType 기준으로 처리
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final CategoryMapper categoryMapper;

    // CAL-04 / CAL-05 일정 목록 조회 (year/month 있으면 월간 조회, categoryId 있으면 필터)
    public List<TaskDTO> getAll(String email, OwnerType ownerType,
                                Integer year, Integer month, Integer categoryId) {
        if (categoryId != null) {
            validateCategoryOwner(categoryId, email, ownerType);
        }

        if (year == null && month == null) {
            if (categoryId != null) {
                return taskMapper.findByOwnerAndCategory(email, ownerType, categoryId);
            }
            return taskMapper.findByOwner(email, ownerType);
        }

        if (year == null || month == null || month < 1 || month > 12) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        return taskMapper.findMonthly(email, ownerType, year, month, categoryId);
    }

    // CAL-08 일정 상세 조회
    public TaskDTO getOne(int taskId, String email, OwnerType ownerType) {
        TaskDTO task = taskMapper.findByIdAndOwner(taskId, email, ownerType);
        if (task == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
        return task;
    }

    // CAL-06 일정 등록
    @Transactional
    public void create(TaskDTO dto, String email, OwnerType ownerType) {
        validateTask(dto);
        validateCategoryOwner(dto.getCategoryId(), email, ownerType);

        dto.setEmail(email);
        dto.setOwnerType(ownerType);
        if (dto.getStatus() == null) {
            dto.setStatus(TaskStatus.IN_PROGRESS);
        }

        int insertedCount = taskMapper.insert(dto);
        if (insertedCount == 0) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // CAL-09 일정 수정
    @Transactional
    public void update(int taskId, TaskDTO dto, String email, OwnerType ownerType) {
        getOne(taskId, email, ownerType);
        validateTask(dto);
        validateCategoryOwner(dto.getCategoryId(), email, ownerType);

        dto.setTaskId(taskId);
        dto.setEmail(email);
        dto.setOwnerType(ownerType);

        int updatedCount = taskMapper.update(dto);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    // CAL-10 일정 삭제
    @Transactional
    public void delete(int taskId, String email, OwnerType ownerType) {
        int deletedCount = taskMapper.deleteByIdAndOwner(taskId, email, ownerType);
        if (deletedCount == 0) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    private void validateCategoryOwner(int categoryId, String email, OwnerType ownerType) {
        if (categoryId <= 0 || categoryMapper.findByIdAndOwner(categoryId, email, ownerType) == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }
    }

    private void validateTask(TaskDTO dto) {
        if (dto == null
                || !StringUtils.hasText(dto.getTitle())
                || dto.getStartAt() == null
                || dto.getEndAt() == null
                || dto.getCategoryId() <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (dto.getEndAt().isBefore(dto.getStartAt())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }
}
