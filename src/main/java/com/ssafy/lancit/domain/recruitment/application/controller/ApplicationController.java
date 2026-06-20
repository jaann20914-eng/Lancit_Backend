package com.ssafy.lancit.domain.recruitment.application.controller;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDetailResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationRequest;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationStatusUpdateRequest;
import com.ssafy.lancit.domain.recruitment.application.service.ApplicationService;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @Operation(summary = "회사용 지원자 목록 조회", description = "회사 토큰이 필요합니다. 목록 조회에서는 viewedAt을 기록하지 않습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ApplicationDetailResponse>>> getApplications(
            @PathVariable int recruitmentId,
            @ModelAttribute PageRequest pageRequest) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getCompanyApplications(recruitmentId, email, role, pageRequest)));
    }

    @Operation(summary = "회사용 지원 상세 조회", description = "회사 토큰이 필요합니다. 최초 상세 조회 시 viewedAt을 기록합니다.")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> getApplication(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getCompanyApplication(recruitmentId, applicationId, email, role)));
    }

    @Operation(summary = "지원서에 제출된 프로젝트 상세 조회",
            description = "공고 작성 회사가 해당 지원서에 선택된 프로젝트만 조회할 수 있습니다.")
    @GetMapping("/{applicationId}/portfolios/{portfolioId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApplicationPortfolio(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId,
            @PathVariable int portfolioId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getCompanyApplicationPortfolio(
                recruitmentId, applicationId, portfolioId, email, role)));
    }

    @Operation(summary = "지원서 프로젝트 파일 URL 조회",
            description = "공고 작성 회사가 해당 지원서에 선택된 프로젝트 파일만 조회할 수 있습니다.")
    @GetMapping("/{applicationId}/portfolios/{portfolioId}/files/{fileId}/url")
    public ResponseEntity<ApiResponse<String>> getApplicationPortfolioFileUrl(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId,
            @PathVariable int portfolioId,
            @PathVariable int fileId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getCompanyApplicationPortfolioFileUrl(
                recruitmentId, applicationId, portfolioId, fileId, email, role)));
    }

    @Operation(summary = "지원서 프로젝트 파일 다운로드 URL 조회",
            description = "공고 작성 회사가 해당 지원서에 선택된 프로젝트 파일만 다운로드할 수 있습니다.")
    @GetMapping("/{applicationId}/portfolios/{portfolioId}/files/{fileId}/download")
    public ResponseEntity<ApiResponse<String>> getApplicationPortfolioFileDownloadUrl(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId,
            @PathVariable int portfolioId,
            @PathVariable int fileId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.getCompanyApplicationPortfolioFileDownloadUrl(
                        recruitmentId, applicationId, portfolioId, fileId, email, role)));
    }

    @Operation(summary = "지원 당시 프로필 사진 URL 조회",
            description = "공고 작성 회사가 지원 당시 프로필 카드의 사진을 조회합니다.")
    @GetMapping("/{applicationId}/profile/image-url")
    public ResponseEntity<ApiResponse<String>> getApplicationProfileImageUrl(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(applicationService.getCompanyApplicationProfileImageUrl(
                recruitmentId, applicationId, email, role)));
    }

    @Operation(summary = "지원 수락/거절", description = "공고 작성 회사만 PENDING 지원을 처리할 수 있습니다.")
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> updateApplicationStatus(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                applicationService.updateStatus(recruitmentId, applicationId, request, email, role)));
    }

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
