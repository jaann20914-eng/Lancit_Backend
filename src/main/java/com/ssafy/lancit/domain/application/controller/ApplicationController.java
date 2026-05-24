package com.ssafy.lancit.domain.application.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.application.dto.ApplicationDTO;
import com.ssafy.lancit.domain.application.service.ApplicationService;

import lombok.RequiredArgsConstructor;
 
@RestController
@RequestMapping("/api/recruitments/{recruitmentId}")
@RequiredArgsConstructor
public class ApplicationController {
 
    private final ApplicationService applicationService;
 
    /** APPLY-03 공고문 지원 (프리랜서)
     *  - RecruitmentApplication 삽입 + PortfolioPermission 삽입 */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Void>> apply(@PathVariable int recruitmentId,
                                                   @RequestBody ApplicationDTO dto) {
        // TODO 영은: applicationService.apply(recruitmentId, dto, email)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-APPLY-03 지원자 목록 조회 (회사 전용) */
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<ApplicationDTO>>> getApplications(
            @PathVariable int recruitmentId) {
        // TODO 영은: applicationService.getList(recruitmentId, companyEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-APPLY-04 지원자 포트폴리오 조회 (회사 전용) */
    @GetMapping("/applications/{applicationId}/portfolios")
    public ResponseEntity<ApiResponse<?>> getApplicantPortfolios(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId) {
        // TODO 영은: applicationService.getPermittedPortfolios(applicationId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-APPLY-03 지원자 찜 토글 (회사 전용) */
    @PostMapping("/applications/{applicationId}/bookmark")
    public ResponseEntity<ApiResponse<Void>> toggleBookmark(
            @PathVariable int recruitmentId,
            @PathVariable int applicationId) {
        // TODO 영은: applicationService.toggleBookmark(applicationId, companyEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
