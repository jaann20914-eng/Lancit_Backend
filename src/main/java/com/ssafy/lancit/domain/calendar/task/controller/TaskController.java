package com.ssafy.lancit.domain.calendar.task.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.service.TaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/calendar/tasks")
@RequiredArgsConstructor
public class TaskController {
 
    private final TaskService taskService;
 
    /** CAL-04 / CLI-CAL-04 일정 목록 조회 (월간) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Integer categoryId) {
        // TODO 영은: taskService.getMonthly(email, ownerType, year, month, categoryId)
        //   categoryId 있으면 CAL-05 카테고리 필터 조회
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CAL-08 / CLI-CAL-08 일정 상세 조회 */
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable int taskId) {
        // TODO 영은: taskService.getOne(taskId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CAL-06 / CLI-CAL-06 일정 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTask(@RequestBody TaskDTO dto) {
        // TODO 영은: taskService.create(dto, email, ownerType)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CAL-07 / CLI-CAL-07 텍스트 파싱 자동 등록 */
    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<Void>> parseAndCreateTask(@RequestBody TaskDTO dto) {
        // TODO 영은: taskService.createFromParsed(dto) → autoRegistered=true 세팅
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CAL-09 / CLI-CAL-09 일정 수정 (@OwnerCheck) */
    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> updateTask(@PathVariable int taskId,
                                                        @RequestBody TaskDTO dto) {
        // TODO 영은: taskService.update(taskId, dto)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CAL-10 / CLI-CAL-10 일정 삭제 (@OwnerCheck) */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable int taskId) {
        // TODO 영은: taskService.delete(taskId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
