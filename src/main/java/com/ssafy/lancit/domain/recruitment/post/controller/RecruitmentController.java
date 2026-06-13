package com.ssafy.lancit.domain.recruitment.post.controller;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentCreateRequest;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDetailResponse;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentListItemResponse;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentSearchCondition;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentStatusUpdateRequest;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentUpdateRequest;
import com.ssafy.lancit.domain.recruitment.post.service.RecruitmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruitments", description = "공고 등록/조회/수정/삭제 API")
@RestController
@RequestMapping("/api/recruitments")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    @Operation(
            summary = "공고 목록 조회",
            description = "구인페이지 목록을 조회합니다. tab은 ALL, APPLIED, BOOKMARKED를 지원하며 기본값은 ALL입니다. "
                    + "jobCategory로 직종 필터를 적용할 수 있고 sort는 LATEST, DEADLINE, BUDGET을 지원합니다. "
                    + "APPLIED/BOOKMARKED 탭은 프리랜서 토큰이 필요합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecruitmentListItemResponse>>> getRecruitments(
            @ModelAttribute RecruitmentSearchCondition condition,
            @ModelAttribute PageRequest pageRequest) {
        CurrentViewer viewer = currentViewerOrAnonymous();
        return ResponseEntity.ok(ApiResponse.ok(
                recruitmentService.getList(condition, pageRequest, viewer.email(), viewer.role())));
    }

    @Operation(summary = "내 공고 목록 조회")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<RecruitmentListItemResponse>>> getMyRecruitments(
            @ModelAttribute RecruitmentSearchCondition condition,
            @ModelAttribute PageRequest pageRequest) {
        CurrentViewer viewer = requiredViewer();
        return ResponseEntity.ok(ApiResponse.ok(
                recruitmentService.getMyList(viewer.email(), viewer.role(), condition, pageRequest)));
    }

    @Operation(summary = "공고 상세 조회")
    @GetMapping("/{recruitmentId}")
    public ResponseEntity<ApiResponse<RecruitmentDetailResponse>> getRecruitment(
            @PathVariable int recruitmentId) {
        CurrentViewer viewer = currentViewerOrAnonymous();
        return ResponseEntity.ok(ApiResponse.ok(
                recruitmentService.getOne(recruitmentId, viewer.email(), viewer.role())));
    }

    @Operation(summary = "공고 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<RecruitmentDetailResponse>> createRecruitment(
            @Valid @RequestBody RecruitmentCreateRequest request) {
        CurrentViewer viewer = requiredViewer();
        return ResponseEntity.ok(ApiResponse.ok(
                recruitmentService.create(request, viewer.email(), viewer.role())));
    }

    @Operation(summary = "공고 수정")
    @PutMapping("/{recruitmentId}")
    public ResponseEntity<ApiResponse<RecruitmentDetailResponse>> updateRecruitment(
            @PathVariable int recruitmentId,
            @Valid @RequestBody RecruitmentUpdateRequest request) {
        CurrentViewer viewer = requiredViewer();
        return ResponseEntity.ok(ApiResponse.ok(
                recruitmentService.update(recruitmentId, request, viewer.email(), viewer.role())));
    }

    @Operation(summary = "공고 삭제")
    @DeleteMapping("/{recruitmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecruitment(@PathVariable int recruitmentId) {
        CurrentViewer viewer = requiredViewer();
        recruitmentService.delete(recruitmentId, viewer.email(), viewer.role());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "공고 상태 변경")
    @PatchMapping("/{recruitmentId}/status")
    public ResponseEntity<ApiResponse<RecruitmentDetailResponse>> updateRecruitmentStatus(
            @PathVariable int recruitmentId,
            @Valid @RequestBody RecruitmentStatusUpdateRequest request) {
        CurrentViewer viewer = requiredViewer();
        return ResponseEntity.ok(ApiResponse.ok(
                recruitmentService.updateStatus(recruitmentId, request, viewer.email(), viewer.role())));
    }

    @Operation(summary = "재등록 소스 조회")
    @GetMapping("/{recruitmentId}/copy-source")
    public ResponseEntity<ApiResponse<RecruitmentCreateRequest>> getCopySource(
            @PathVariable int recruitmentId) {
        CurrentViewer viewer = requiredViewer();
        return ResponseEntity.ok(ApiResponse.ok(
                recruitmentService.getCopySource(recruitmentId, viewer.email(), viewer.role())));
    }

    private CurrentViewer requiredViewer() {
        String role = SecurityUtil.getCurrentRole();
        String email = SecurityUtil.getCurrentEmail();
        return new CurrentViewer(email, role);
    }

    private CurrentViewer currentViewerOrAnonymous() {
        try {
            return requiredViewer();
        } catch (CustomException e) {
            if (ErrorCode.UNAUTHORIZED.equals(e.getErrorCode())) {
                return new CurrentViewer(null, null);
            }
            throw e;
        }
    }

    private record CurrentViewer(String email, String role) {
    }
}
