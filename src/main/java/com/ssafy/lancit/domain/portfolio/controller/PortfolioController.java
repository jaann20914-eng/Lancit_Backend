package com.ssafy.lancit.domain.portfolio.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileUpdateRequest;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioSearchCondition;
import com.ssafy.lancit.domain.portfolio.service.PortfolioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

// 포트폴리오 CRUD - 프리랜서 전용
// 배너 이미지는 FileController 에서 별도 처리 (프론트가 /api/files/{bannerFileId}/url 별도 호출)
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "프로젝트 및 포트폴리오 공개 프로필 API")
public class PortfolioController {

    private final PortfolioService portfolioService;

    // PORT-01 내 포트폴리오 목록 조회 (페이지네이션)
    @Operation(summary = "내 프로젝트 목록 조회", description = "프리랜서 본인의 프로젝트 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDTO>>> getMyPortfolios(
            @ModelAttribute PageRequest pageRequest,
            @ModelAttribute PortfolioSearchCondition condition) {
        String email = SecurityUtil.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getMyList(email, pageRequest, condition)));
    }

    // PORT-PROFILE-01 내 포트폴리오 프로필 카드 조회
    @Operation(summary = "내 포트폴리오 프로필 카드 조회", description = "프리랜서만 접근할 수 있으며, 공개 여부가 false여도 본인은 조회할 수 있습니다.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PortfolioProfileDTO>> getMyProfile() {
        String email = SecurityUtil.getCurrentEmail();
        if (!"USER".equals(SecurityUtil.getCurrentRole())) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getMyProfile(email)));
    }

    // PORT-PROFILE-02 내 포트폴리오 프로필 카드 저장
    @Operation(summary = "내 포트폴리오 프로필 카드 저장", description = "프리랜서만 수정할 수 있습니다. partial update가 아니라 화면 폼의 현재 값을 전체 저장합니다.")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<PortfolioProfileDTO>> updateMyProfile(
            @RequestBody PortfolioProfileUpdateRequest request) {
        String email = SecurityUtil.getCurrentEmail();
        if (!"USER".equals(SecurityUtil.getCurrentRole())) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.updateMyProfile(email, request)));
    }

    // CLI-SEAR-02 회사가 특정 프리랜서 공개 포트폴리오 조회 (페이지네이션)
    @Operation(summary = "공개 프로젝트 목록 조회", description = "포트폴리오 전체 공개 상태인 프리랜서의 공개 프로젝트만 조회합니다.")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDTO>>> getPublicPortfolios(
            @RequestParam String email,
            @ModelAttribute PageRequest pageRequest,
            @ModelAttribute PortfolioSearchCondition condition) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getPublicList(email, pageRequest, condition)));
    }

    // PORT-PROFILE-03 공개 포트폴리오 프로필 + 공개 프로젝트 조회
    @Operation(summary = "공개 포트폴리오 프로필 조회", description = "포트폴리오 전체 공개 상태인 프리랜서의 공개 프로필과 공개 프로젝트만 조회합니다.")
    @GetMapping("/public/{freelancerEmail}")
    public ResponseEntity<ApiResponse<PortfolioProfileDTO>> getPublicProfile(
            @PathVariable String freelancerEmail) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getPublicProfile(freelancerEmail)));
    }

	 // PORT-02 / CLI-SEAR-03 포트폴리오 상세 조회
	 // bannerFileId → 프론트가 /api/files/{bannerFileId}/url 로 Signed URL 별도 조회
	 // files → 결과물 파일 목록 포함
     @Operation(summary = "프로젝트 상세 조회", description = "프로젝트 단건과 결과물 파일 목록을 조회합니다.")
	 @GetMapping("/{portfolioId}")
	 public ResponseEntity<ApiResponse<Map<String, Object>>> getPortfolio(@PathVariable int portfolioId) {
	     return ResponseEntity.ok(ApiResponse.ok(portfolioService.getOne(portfolioId)));
	 }

    // PORT-03 포트폴리오 등록
    // 배너 이미지 업로드 흐름:
    //   1. POST /api/files/upload → bannerFileId 반환
    //   2. dto.bannerFileId 에 담아서 이 API 호출
    @Operation(summary = "프로젝트 등록", description = "프리랜서만 등록할 수 있습니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createPortfolio(@RequestBody PortfolioDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        if (!"USER".equals(SecurityUtil.getCurrentRole())) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
        portfolioService.create(dto, email);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PORT-03 포트폴리오 수정 (@OwnerCheck 서비스에서 처리)
    @Operation(summary = "프로젝트 전체 수정", description = "partial update가 아니라 생성과 유사한 전체 수정 정책을 유지합니다.")
    @PutMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> updatePortfolio(@PathVariable int portfolioId,
                                                              @RequestBody PortfolioDTO dto) {
        portfolioService.update(portfolioId, dto);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PORT-04 포트폴리오 삭제 (@OwnerCheck 서비스에서 처리)
    // GCS 파일 + Redis Signed URL 캐시는 PortfolioService 에서 처리
    @Operation(summary = "프로젝트 삭제", description = "soft delete 정책으로 삭제합니다.")
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(@PathVariable int portfolioId) {
        portfolioService.delete(portfolioId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
