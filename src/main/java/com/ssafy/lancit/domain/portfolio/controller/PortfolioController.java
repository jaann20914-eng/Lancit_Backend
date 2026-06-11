package com.ssafy.lancit.domain.portfolio.controller;

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

import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioSearchCondition;
import com.ssafy.lancit.domain.portfolio.service.PortfolioService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 포트폴리오 CRUD - 화면에서는 프로젝트로 노출된다.
@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "내 프로젝트 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDTO>>> getMyPortfolios(
            @ModelAttribute PortfolioSearchCondition condition) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getMyList(email, role, condition)));
    }

    @Operation(summary = "공개 프로젝트 목록 조회")
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PageResponse<PortfolioDTO>>> getPublicPortfolios(
            @RequestParam String email,
            @ModelAttribute PortfolioSearchCondition condition) {
        String currentEmail = SecurityUtil.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getPublicList(email, currentEmail, condition)));
    }

    @Operation(summary = "프로젝트 상세 조회")
    @GetMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getPortfolio(@PathVariable int portfolioId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.getOne(portfolioId, email, role)));
    }

    @Operation(summary = "프로젝트 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<PortfolioDTO>> createPortfolio(@Valid @RequestBody PortfolioDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.create(dto, email, role)));
    }

    @Operation(
            summary = "프로젝트 수정",
            description = "프로젝트를 전체 수정합니다. 요청 본문에 category, title, summary, content를 모두 포함해야 합니다."
    )
    @PutMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioDTO>> updatePortfolio(@PathVariable int portfolioId,
                                                                     @Valid @RequestBody PortfolioDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(portfolioService.update(portfolioId, dto, email, role)));
    }

    @Operation(summary = "프로젝트 삭제")
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(@PathVariable int portfolioId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        portfolioService.delete(portfolioId, email, role);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
