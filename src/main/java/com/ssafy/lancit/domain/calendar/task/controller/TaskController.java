package com.ssafy.lancit.domain.calendar.task.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.service.TaskParseService;
import com.ssafy.lancit.domain.calendar.task.service.TaskService;
import com.ssafy.lancit.global.enums.OwnerType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// 캘린더 일정 CRUD - 프리랜서(USER) / 회사(COMPANY) 공용
// 납기일 알림(CAL-11)은 TaskScheduler 가 매일 오전 9시 자동 실행
// → STOMP /sub/notification/{email} 으로 푸시 + Redis 중복 방지
@RestController
@RequestMapping("/api/calendar/tasks")
@RequiredArgsConstructor
@Tag(name = "Calendar Task", description = "캘린더 일정 API")
public class TaskController {

    private final TaskService taskService;
    private final TaskParseService taskParseService;

    // CAL-04 / CLI-CAL-04 일정 조회 (year/month 월간, startDate/endDate 기간, categoryId 필터)
    @GetMapping
    @Operation(summary = "일정 목록 조회", description = "year/month를 주면 월간 조회, startDate/endDate를 주면 기간 조회, 모두 생략하면 전체 조회합니다.")
    public ResponseEntity<ApiResponse<List<TaskDTO>>> getTasks(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer categoryId) {
        String email = SecurityUtil.getCurrentEmail();
        OwnerType ownerType = getCurrentOwnerType();
        return ResponseEntity.ok(ApiResponse.ok(
                taskService.getTasks(email, ownerType, year, month, startDate, endDate, categoryId)
        ));
    }

    // CAL-08 / CLI-CAL-08 일정 상세 조회
    @GetMapping("/{taskId}")
    @Operation(summary = "일정 상세 조회")
    public ResponseEntity<ApiResponse<TaskDTO>> getTask(@PathVariable int taskId) {
        return ResponseEntity.ok(ApiResponse.ok(taskService.getOne(taskId)));
    }

    // CAL-06 / CLI-CAL-06 일정 등록
    @PostMapping
    @Operation(summary = "일정 등록")
    public ResponseEntity<ApiResponse<Void>> createTask(@RequestBody TaskDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        OwnerType ownerType = getCurrentOwnerType();
        taskService.create(dto, email, ownerType);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-07 / CLI-CAL-07 텍스트 파싱 미리보기
    // DB 저장 없이 오른쪽 일정 입력 폼 자동 채우기용 DTO 만 반환
    @PostMapping("/parse")
    @Operation(summary = "일정 텍스트 파싱")
    public ResponseEntity<ApiResponse<TaskParseResponseDTO>> parseTask(
            @RequestBody TaskParseRequestDTO requestDTO) {
        TaskParseResponseDTO responseDTO = taskParseService.parse(requestDTO);
        return ResponseEntity.ok(ApiResponse.ok(responseDTO));
    }

    // CAL-09 / CLI-CAL-09 일정 수정 (서비스 내부에서 소유자 검증)
    @PutMapping("/{taskId}")
    @Operation(summary = "일정 수정")
    public ResponseEntity<ApiResponse<Void>> updateTask(@PathVariable int taskId,
                                                        @RequestBody TaskDTO dto) {
        taskService.update(taskId, dto);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CAL-10 / CLI-CAL-10 일정 삭제 (서비스 내부에서 소유자 검증)
    @DeleteMapping("/{taskId}")
    @Operation(summary = "일정 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable int taskId) {
        taskService.delete(taskId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private OwnerType getCurrentOwnerType() {
        return "USER".equals(SecurityUtil.getCurrentRole()) ? OwnerType.USER : OwnerType.COMPANY;
    }
}
