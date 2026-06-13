package com.ssafy.lancit.domain.recruitment.application.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDetailResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationRequest;
import com.ssafy.lancit.domain.recruitment.application.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruitment Applications", description = "프리랜서 공고 지원 API")
@RestController
@RequestMapping("/api/recruitments/{recruitmentId}/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @Operation(summary = "공고 지원 등록", description = "프리랜서 토큰이 필요합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> apply(
            @PathVariable int recruitmentId,
            @Valid @RequestBody ApplicationRequest request) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.apply(recruitmentId, request, email, role)));
    }

    @Operation(summary = "내 지원 상세 조회", description = "프리랜서 토큰이 필요합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> getMyApplication(
            @PathVariable int recruitmentId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getMine(recruitmentId, email, role)));
    }

    @Operation(summary = "내 지원 수정", description = "프리랜서 토큰이 필요합니다. 요청값으로 전체 교체합니다.")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> updateMyApplication(
            @PathVariable int recruitmentId,
            @Valid @RequestBody ApplicationRequest request) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.updateMine(recruitmentId, request, email, role)));
    }

    @Operation(summary = "내 지원 취소", description = "프리랜서 토큰이 필요합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> cancelMyApplication(@PathVariable int recruitmentId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        applicationService.cancelMine(recruitmentId, email, role);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
