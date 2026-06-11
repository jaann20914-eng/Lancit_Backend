package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.dto.CategoryDTO;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.global.enums.OwnerType;
import com.ssafy.lancit.global.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

// 캘린더 일정 CRUD
// 납기일 알림(CAL-11)은 TaskScheduler 가 담당 (STOMP + Redis)
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final CategoryMapper categoryMapper;

    // CAL-04 / CAL-05 일정 목록 조회 (월간/기간/categoryId 조건)
    public List<TaskDTO> getTasks(String email, OwnerType ownerType,
                                  Integer year, Integer month,
                                  LocalDate startDate, LocalDate endDate,
                                  Integer categoryId) {
        validateCategoryFilter(categoryId, email, ownerType);
        DateRange dateRange = resolveDateRange(year, month, startDate, endDate);
        return taskMapper.findByCondition(
                email,
                ownerType,
                dateRange.start(),
                dateRange.endExclusive(),
                categoryId
        );
    }

    // CAL-08 일정 상세 조회
    public TaskDTO getOne(int taskId) {
        String email = SecurityUtil.getCurrentEmail();
        OwnerType ownerType = getCurrentOwnerType();
        return validateTaskOwner(taskId, email, ownerType);
    }

    // CAL-06 일정 등록
    @Transactional
    public void create(TaskDTO dto, String email, OwnerType ownerType) {
        validateCreate(dto);
        validateCategoryOwner(dto.getCategoryId(), email, ownerType);

        dto.setEmail(email);
        dto.setOwnerType(ownerType);
        if (dto.getStatus() == null) {
            dto.setStatus(TaskStatus.IN_PROGRESS);
        }
        if (dto.getBudget() == null) {
            dto.setBudget(0);
        }
        if (dto.getAutoRegistered() == null) {
            dto.setAutoRegistered(false);
        }
        taskMapper.insert(dto);
    }

    // 텍스트 파싱 결과는 자동 저장하지 않는다.
    // /api/calendar/tasks/parse 에서 TaskParseResponseDTO만 반환하고,
    // 사용자가 확인/수정한 뒤 일반 일정 등록 API를 통해 저장한다.
    @Transactional
    public void createFromParsed(TaskDTO dto, String email, OwnerType ownerType) {
        // 파싱 API = 미리보기 전용
        // 일반 일정 등록 API = 최종 저장 전용
    }

    // CAL-09 일정 수정 (서비스 내부에서 소유자 검증)
    @Transactional
    public void update(int taskId, TaskDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        OwnerType ownerType = getCurrentOwnerType();
        TaskDTO existingTask = validateTaskOwner(taskId, email, ownerType);

        validateUpdate(dto, existingTask);
        if (dto.getCategoryId() != null) {
            validateCategoryOwner(dto.getCategoryId(), email, ownerType);
        }
        int updatedCount = taskMapper.update(taskId, dto, email, ownerType);
        if (updatedCount == 0) {
            throw new CustomException(ErrorCode.TASK_NOT_FOUND);
        }
    }

    // CAL-10 일정 삭제 (서비스 내부에서 소유자 검증)
    @Transactional
    public void delete(int taskId) {
        String email = SecurityUtil.getCurrentEmail();
        OwnerType ownerType = getCurrentOwnerType();

        validateTaskOwner(taskId, email, ownerType);

        int deletedCount = taskMapper.deleteByIdAndOwner(taskId, email, ownerType);
        if (deletedCount == 0) {
            throw new CustomException(ErrorCode.TASK_NOT_FOUND);
        }
    }

    private DateRange resolveDateRange(Integer year, Integer month,
                                       LocalDate startDate, LocalDate endDate) {
        boolean monthlyRequested = year != null || month != null;
        boolean rangeRequested = startDate != null || endDate != null;

        if (monthlyRequested && rangeRequested) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (monthlyRequested) {
            if (year == null || month == null || month < 1 || month > 12) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime rangeEndExclusive = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
            return new DateRange(rangeStart, rangeEndExclusive);
        }
        if (rangeRequested) {
            if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            return new DateRange(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        }
        return new DateRange(null, null);
    }

    private void validateCreate(TaskDTO dto) {
        if (dto == null
                || dto.getCategoryId() == null
                || dto.getCategoryId() <= 0
                || isBlank(dto.getTitle())
                || dto.getStartAt() == null
                || dto.getEndAt() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        validatePeriod(dto.getStartAt(), dto.getEndAt());
        validateBudget(dto.getBudget());
    }

    private void validateUpdate(TaskDTO dto, TaskDTO existingTask) {
        if (dto == null || !hasUpdateField(dto)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getCategoryId() != null && dto.getCategoryId() <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (dto.getTitle() != null && isBlank(dto.getTitle())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        validateBudget(dto.getBudget());

        LocalDateTime startAt = dto.getStartAt() == null ? existingTask.getStartAt() : dto.getStartAt();
        LocalDateTime endAt = dto.getEndAt() == null ? existingTask.getEndAt() : dto.getEndAt();
        validatePeriod(startAt, endAt);
    }

    private boolean hasUpdateField(TaskDTO dto) {
        return dto.getCategoryId() != null
                || dto.getTitle() != null
                || dto.getContent() != null
                || dto.getMemo() != null
                || dto.getStatus() != null
                || dto.getStartAt() != null
                || dto.getEndAt() != null
                || dto.getClientCompany() != null
                || dto.getBudget() != null
                || dto.getPaidAt() != null
                || dto.getAutoRegistered() != null
                || dto.getAutoRegisteredSource() != null;
    }

    private void validateCategoryFilter(Integer categoryId, String email, OwnerType ownerType) {
        if (categoryId == null) {
            return;
        }
        validateCategoryOwner(categoryId, email, ownerType);
    }

    private CategoryDTO validateCategoryOwner(Integer categoryId, String email, OwnerType ownerType) {
        if (categoryId == null || categoryId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        CategoryDTO category = categoryMapper.findByIdAndOwner(categoryId, email, ownerType);
        if (category == null) {
            throw new CustomException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private TaskDTO validateTaskOwner(Integer taskId, String email, OwnerType ownerType) {
        if (taskId == null || taskId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        TaskDTO task = taskMapper.findByIdAndOwner(taskId, email, ownerType);
        if (task == null) {
            throw new CustomException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && startAt.isAfter(endAt)) {
            throw new CustomException(ErrorCode.INVALID_TASK_PERIOD);
        }
    }

    private void validateBudget(Integer budget) {
        if (budget != null && budget < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private OwnerType getCurrentOwnerType() {
        return OwnerType.fromRole(SecurityUtil.getCurrentRole());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record DateRange(LocalDateTime start, LocalDateTime endExclusive) {
    }
}
