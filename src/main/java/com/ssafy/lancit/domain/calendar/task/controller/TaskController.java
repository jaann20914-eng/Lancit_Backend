package com.ssafy.lancit.domain.calendar.task.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.service.TaskParseService;
import com.ssafy.lancit.domain.calendar.task.service.TaskService;
import com.ssafy.lancit.global.enums.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 캘린더 일정 CRUD - 프리랜서(USER) / 회사(COMPANY) 공용
// 납기일 알림(CAL-11)은 TaskScheduler 가 매일 오전 9시 자동 실행
// → STOMP /sub/notification/{email} 으로 푸시 + Redis 중복 방지
@RestController
@RequestMapping("/api/calendar/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskParseService taskParseService;

    // CAL-04 / CLI-CAL-04 월간 일정 조회 (categoryId 있으면 CAL-05 카테고리 필터)
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Integer categoryId) {
        // TODO 영은 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: OwnerType ownerType = "USER".equals(SecurityUtil.getCurrentRole()) ? OwnerType.USER : OwnerType.COMPANY
        // TODO 영은 [3]: return ResponseEntity.ok(ApiResponse.ok(taskService.getMonthly(email, ownerType, year, month, categoryId)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-08 / CLI-CAL-08 일정 상세 조회
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable int taskId) {
        // TODO 영은 [1]: return ResponseEntity.ok(ApiResponse.ok(taskService.getOne(taskId)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-06 / CLI-CAL-06 일정 등록
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createTask(@RequestBody TaskDTO dto) {
        // TODO 영은 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: OwnerType ownerType = "USER".equals(SecurityUtil.getCurrentRole()) ? OwnerType.USER : OwnerType.COMPANY
        // TODO 영은 [3]: taskService.create(dto, email, ownerType)
        // TODO 영은 [4]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-07 / CLI-CAL-07 텍스트 파싱 미리보기
    // DB 저장 없이 오른쪽 일정 입력 폼 자동 채우기용 DTO 만 반환
    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<TaskParseResponseDTO>> parseTask(
            @RequestBody TaskParseRequestDTO requestDTO) {
        TaskParseResponseDTO responseDTO = taskParseService.parse(requestDTO);
        return ResponseEntity.ok(ApiResponse.ok(responseDTO));
    }

    // CAL-09 / CLI-CAL-09 일정 수정 (@OwnerCheck 서비스에서 처리)
    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> updateTask(@PathVariable int taskId,
                                                        @RequestBody TaskDTO dto) {
        // TODO 영은 [1]: taskService.update(taskId, dto)
        // TODO 영은 [2]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-10 / CLI-CAL-10 일정 삭제 (@OwnerCheck 서비스에서 처리)
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable int taskId) {
        // TODO 영은 [1]: taskService.delete(taskId)
        // TODO 영은 [2]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
