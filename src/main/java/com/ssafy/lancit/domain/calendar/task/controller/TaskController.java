package com.ssafy.lancit.domain.calendar.task.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.service.TaskService;
import com.ssafy.lancit.global.enums.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 캘린더 일정 CRUD
// 1차 구현은 로그인한 USER 기준으로 처리하고, COMPANY 캘린더는 추후 확장 예정
@RestController
@RequestMapping("/api/calendar/tasks")
@RequiredArgsConstructor
public class TaskController {

    private static final OwnerType DAY_ONE_OWNER_TYPE = OwnerType.USER;

    private final TaskService taskService;

    // CAL-04 / CLI-CAL-04 일정 목록 조회 (year/month 있으면 월간 조회, categoryId 있으면 필터)
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer categoryId) {
        String email = SecurityUtil.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(
                taskService.getAll(email, DAY_ONE_OWNER_TYPE, year, month, categoryId)
        ));
    }

    // CAL-08 / CLI-CAL-08 일정 상세 조회
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable int taskId) {
        String email = SecurityUtil.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(taskService.getOne(taskId, email, DAY_ONE_OWNER_TYPE)));
    }

    // CAL-06 / CLI-CAL-06 일정 등록
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTask(@RequestBody TaskDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        taskService.create(dto, email, DAY_ONE_OWNER_TYPE);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-09 / CLI-CAL-09 일정 수정
    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> updateTask(@PathVariable int taskId,
                                                        @RequestBody TaskDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        taskService.update(taskId, dto, email, DAY_ONE_OWNER_TYPE);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-10 / CLI-CAL-10 일정 삭제
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable int taskId) {
        String email = SecurityUtil.getCurrentEmail();
        taskService.delete(taskId, email, DAY_ONE_OWNER_TYPE);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
