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

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioCreateResponse;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileUpdateRequest;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioSearchCondition;
import com.ssafy.lancit.domain.portfolio.service.PortfolioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 포트폴리오 CRUD - 프리랜서 전용
// 배너 이미지는 FileController 에서 별도 처리 (프론트가 /api/files/{bannerFileId}/url 별도 호출)
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final FileService fileService;

    // PORT-01 내 포트폴리오 목록 조회 (페이지네이션)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDTO>>> getMyPortfolios(
            @ModelAttribute PageRequest pageRequest,
            @ModelAttribute PortfolioSearchCondition condition) {
        String email = SecurityUtil.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getMyList(email, pageRequest, condition)));
    }

    // PORT-PROFILE-01 내 포트폴리오 프로필 카드 조회
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PortfolioProfileDTO>> getMyProfile() {
        String email = SecurityUtil.getCurrentEmail();
        if (!"USER".equals(SecurityUtil.getCurrentRole())) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getMyProfile(email)));
    }

    // PORT-PROFILE-02 내 포트폴리오 프로필 카드 저장
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<PortfolioProfileDTO>> updateMyProfile(
            @Valid @RequestBody PortfolioProfileUpdateRequest request) {
        String email = SecurityUtil.getCurrentEmail();
        if (!"USER".equals(SecurityUtil.getCurrentRole())) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.updateMyProfile(email, request)));
    }

    // CLI-SEAR-02 회사가 특정 프리랜서 공개 포트폴리오 조회 (페이지네이션)
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDTO>>> getPublicPortfolios(
            @RequestParam String email,
            @ModelAttribute PageRequest pageRequest,
            @ModelAttribute PortfolioSearchCondition condition) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getPublicList(email, pageRequest, condition)));
    }

	 // PORT-02 / CLI-SEAR-03 포트폴리오 상세 조회
	 // bannerFileId → 프론트가 /api/files/{bannerFileId}/url 로 Signed URL 별도 조회
	 // files → 결과물 파일 목록 포함
	 @GetMapping("/{portfolioId}")
		 public ResponseEntity<ApiResponse<Map<String, Object>>> getPortfolio(@PathVariable int portfolioId) {
		     String email = SecurityUtil.getCurrentEmail();
		     return ResponseEntity.ok(ApiResponse.ok(portfolioService.getOneForViewer(portfolioId, email)));
		 }

    // PORT-03 포트폴리오 등록
    // 배너 이미지 업로드 흐름:
    //   1. POST /api/files/upload → bannerFileId 반환
    //   2. dto.bannerFileId 에 담아서 이 API 호출
    @PostMapping
    public ResponseEntity<ApiResponse<PortfolioCreateResponse>> createPortfolio(@RequestBody PortfolioDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        if (!"USER".equals(SecurityUtil.getCurrentRole())) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
        Integer portfolioId = portfolioService.create(dto, email);
        return ResponseEntity.ok(ApiResponse.ok(new PortfolioCreateResponse(portfolioId)));
    }

    // PORT-03 포트폴리오 수정 (@OwnerCheck 서비스에서 처리)
    @PutMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> updatePortfolio(@PathVariable int portfolioId,
                                                              @RequestBody PortfolioDTO dto) {
        portfolioService.update(portfolioId, dto);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PORT-04 포트폴리오 삭제 (@OwnerCheck 서비스에서 처리)
    // GCS 파일 + Redis Signed URL 캐시는 PortfolioService 에서 처리
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(@PathVariable int portfolioId) {
        portfolioService.delete(portfolioId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
    
    

    //------------------------지원 
    @GetMapping("/{fileId}/public-url")
    public ResponseEntity<ApiResponse<String>> getPublicUrl(@PathVariable int fileId) {
        String url = fileService.getSignedUrl(fileId);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }
    @GetMapping("/profile/public")
    public ResponseEntity<ApiResponse<PortfolioProfileDTO>> getPublicProfile(
            @RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getFreelancerProfile(email)));
    }
    
}
