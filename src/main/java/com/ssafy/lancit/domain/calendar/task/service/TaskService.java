package com.ssafy.lancit.domain.calendar.task.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.annotation.OwnerCheck;
import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {
 
    private final TaskMapper taskMapper;
 
    /** CAL-04 / CAL-05 월간 일정 조회 (카테고리 필터 포함) */
    public List<TaskDTO> getMonthly(String email, int year, int month, Integer categoryId) {
        // TODO 영은: taskMapper.findMonthly(email, year, month, categoryId)
        return null;
    }
 
    /** CAL-08 일정 상세 조회 */
    public TaskDTO getOne(int taskId) {
        // TODO 영은: taskMapper.findById(taskId)
        return null;
    }
 
    /** CAL-06 일정 등록 */
    @Transactional
    public void create(TaskDTO dto) {
        // TODO 영은: email + ownerType 세팅 → taskMapper.insert(dto)
    }
 
    /** CAL-07 텍스트 파싱 자동 등록 */
    @Transactional
    public void createFromParsed(TaskDTO dto) {
        // TODO 영은: autoRegistered=true, autoRegisteredSource 세팅 → taskMapper.insert(dto)
    }
 
    /** CAL-09 일정 수정 (@OwnerCheck) */
    @OwnerCheck(resourceType = "TASK")
    @Transactional
    public void update(int taskId, TaskDTO dto) {
        // TODO 영은: taskMapper.update(taskId, dto)
    }
 
    /** CAL-10 일정 삭제 (@OwnerCheck) */
    @OwnerCheck(resourceType = "TASK")
    @Transactional
    public void delete(int taskId) {
        // TODO 영은: taskMapper.delete(taskId)
    }
}