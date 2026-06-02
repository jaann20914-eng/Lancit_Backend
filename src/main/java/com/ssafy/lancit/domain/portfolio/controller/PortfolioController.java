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
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.service.PortfolioService;

import lombok.RequiredArgsConstructor;

// 포트폴리오 CRUD - 프리랜서 전용
// 배너 이미지는 FileController 에서 별도 처리 (프론트가 /api/files/{bannerFileId}/url 별도 호출)
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    // PORT-01 내 포트폴리오 목록 조회 (페이지네이션)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDTO>>> getMyPortfolios(
            @ModelAttribute PageRequest pageRequest) {
        // TODO 영은 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: return ResponseEntity.ok(ApiResponse.ok(portfolioService.getMyList(email, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-SEAR-02 회사가 특정 프리랜서 공개 포트폴리오 조회 (페이지네이션)
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDTO>>> getPublicPortfolios(
            @RequestParam String email,
            @ModelAttribute PageRequest pageRequest) {
        // TODO 영은 [1]: return ResponseEntity.ok(ApiResponse.ok(portfolioService.getPublicList(email, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

	 // PORT-02 / CLI-SEAR-03 포트폴리오 상세 조회
	 // bannerFileId → 프론트가 /api/files/{bannerFileId}/url 로 Signed URL 별도 조회
	 // files → 결과물 파일 목록 포함
	 @GetMapping("/{portfolioId}")
	 public ResponseEntity<ApiResponse<Map<String, Object>>> getPortfolio(@PathVariable int portfolioId) {
	     // TODO 영은 [1]: return ResponseEntity.ok(ApiResponse.ok(portfolioService.getOne(portfolioId)))
	     return ResponseEntity.ok(ApiResponse.ok(null));
	 }

    // PORT-03 포트폴리오 등록
    // 배너 이미지 업로드 흐름:
    //   1. POST /api/files/upload → bannerFileId 반환
    //   2. dto.bannerFileId 에 담아서 이 API 호출
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createPortfolio(@RequestBody PortfolioDTO dto) {
        // TODO 영은 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: portfolioService.create(dto, email)
        // TODO 영은 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PORT-03 포트폴리오 수정 (@OwnerCheck 서비스에서 처리)
    @PutMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> updatePortfolio(@PathVariable int portfolioId,
                                                              @RequestBody PortfolioDTO dto) {
        // TODO 영은 [1]: portfolioService.update(portfolioId, dto)
        // TODO 영은 [2]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // PORT-04 포트폴리오 삭제 (@OwnerCheck 서비스에서 처리)
    // GCS 파일 + Redis Signed URL 캐시는 PortfolioService 에서 처리
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(@PathVariable int portfolioId) {
        // TODO 영은 [1]: portfolioService.delete(portfolioId)
        // TODO 영은 [2]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}