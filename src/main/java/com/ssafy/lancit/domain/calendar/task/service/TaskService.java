package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.global.enums.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 캘린더 일정 CRUD
// 납기일 알림(CAL-11)은 TaskScheduler 가 담당 (STOMP + Redis)
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;

    // CAL-04 / CAL-05 월간 일정 조회 (categoryId 있으면 카테고리 필터)
    public List<TaskDTO> getMonthly(String email, OwnerType ownerType,
                                    int year, int month, Integer categoryId) {
        // TODO 영은 [1]: return taskMapper.findMonthly(email, ownerType, year, month, categoryId)
        return null;
    }

    // CAL-08 일정 상세 조회
    public TaskDTO getOne(int taskId) {
        // TODO 영은 [1]: return taskMapper.findById(taskId)
        return null;
    }

    // CAL-06 일정 등록
    @Transactional
    public void create(TaskDTO dto, String email, OwnerType ownerType) {
        // TODO 영은 [1]: dto.setEmail(email)
        // TODO 영은 [2]: dto.setOwnerType(ownerType)
        // TODO 영은 [3]: taskMapper.insert(dto)
    }

    // CAL-07 텍스트 파싱 자동 등록
    // 텍스트에서 제목/날짜/금액/의뢰회사 추출 후 저장
    @Transactional
    public void createFromParsed(TaskDTO dto, String email, OwnerType ownerType) {
        // TODO 영은 [1]: dto.setEmail(email)
        // TODO 영은 [2]: dto.setOwnerType(ownerType)
        // TODO 영은 [3]: dto.setAutoRegistered(true)
        // TODO 영은 [4]: dto.setAutoRegisteredSource(원본텍스트)
        // TODO 영은 [5]: taskMapper.insert(dto)
    }

    // CAL-09 일정 수정 (@OwnerCheck 로 소유자 검증)
    @OwnerCheck(resourceType = "TASK")
    @Transactional
    public void update(int taskId, TaskDTO dto) {
        // TODO 영은 [1]: taskMapper.update(taskId, dto)
    }

    // CAL-10 일정 삭제 (@OwnerCheck 로 소유자 검증)
    @OwnerCheck(resourceType = "TASK")
    @Transactional
    public void delete(int taskId) {
        // TODO 영은 [1]: taskMapper.delete(taskId)
    }
}