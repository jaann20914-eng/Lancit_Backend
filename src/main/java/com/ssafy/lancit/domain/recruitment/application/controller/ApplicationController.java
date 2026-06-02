package com.ssafy.lancit.domain.recruitment.application.controller;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDTO;
import com.ssafy.lancit.domain.recruitment.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 공고문 지원 관련 - 프리랜서(지원), 회사(지원자 목록/포트폴리오/찜)
@RestController
@RequestMapping("/api/recruitments/{recruitmentId}")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // APPLY-03 공고문 지원 (프리랜서 전용)
    // 지원 시 열람 허용할 포트폴리오 ID 목록 함께 전송 (dto.portfolioIds)
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Void>> apply(@PathVariable int recruitmentId,
                                                   @RequestBody ApplicationDTO dto) {
        // TODO 영은 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: applicationService.apply(recruitmentId, dto, email)
        //               → recruitment_application INSERT + portfolio_permission INSERT
        // TODO 영은 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-APPLY-03 지원자 목록 조회 (회사 전용, 페이지네이션)
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationDTO>>> getApplications(
            @PathVariable int recruitmentId,
            @ModelAttribute PageRequest pageRequest) {
        // TODO 영은 [1]: String companyEmail = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: return ResponseEntity.ok(ApiResponse.ok(
        //               applicationService.getList(recruitmentId, companyEmail, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-APPLY-04 지원자 포트폴리오 조회 (회사 전용, 페이지네이션)
    // 열람 허용된 포트폴리오만 반환 (portfolio_permission 테이블 기준)
    // bannerFileId 포함 → 프론트가 /api/files/{bannerFileId}/url 로 Signed URL 별도 조회
    @GetMapping("/applications/{applicationId}/portfolios")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> getApplicantPortfolios(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId,
            @ModelAttribute PageRequest pageRequest) {
        // TODO 영은 [1]: return ResponseEntity.ok(ApiResponse.ok(
        //               applicationService.getPermittedPortfolios(applicationId, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-APPLY-03 지원자 찜 토글 (회사 전용)
    // bookmark 테이블의 is_bookmarked_by_company 토글
    @PostMapping("/applications/{applicationId}/bookmark")
    public ResponseEntity<ApiResponse<Void>> toggleBookmark(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId) {
        // TODO 영은 [1]: String companyEmail = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: applicationService.toggleBookmark(applicationId, companyEmail)
        // TODO 영은 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}