package com.ssafy.lancit.domain.portfolio.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.service.PortfolioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/portfolios")
@RequiredArgsConstructor
public class PortfolioController {
 
    private final PortfolioService portfolioService;
 
    /** PORT-01 내 포트폴리오 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PortfolioDTO>>> getMyPortfolios() {
        // TODO 영은: portfolioService.getMyList(email)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-SEAR-02 회사가 특정 프리랜서 공개 포트폴리오 조회 */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<PortfolioDTO>>> getPublicPortfolios(
            @RequestParam String email) {
        // TODO 영은: portfolioService.getPublicList(email)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** PORT-02 / CLI-SEAR-03 포트폴리오 상세 조회 */
    @GetMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getPortfolio(@PathVariable int portfolioId) {
        // TODO 영은: portfolioService.getOne(portfolioId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** PORT-03 포트폴리오 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createPortfolio(@RequestBody PortfolioDTO dto) {
        // TODO 영은: portfolioService.create(dto, email)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** PORT-03 포트폴리오 수정 (@OwnerCheck) */
    @PutMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> updatePortfolio(@PathVariable int portfolioId,
                                                              @RequestBody PortfolioDTO dto) {
        // TODO 영은: portfolioService.update(portfolioId, dto)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** PORT-04 포트폴리오 삭제 (@OwnerCheck)
     *  - PortfolioPermission CASCADE 자동 삭제
     *  - 결과물 파일은 FileDeleteEvent 로 GCS 삭제 */
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(@PathVariable int portfolioId) {
        // TODO 영은: portfolioService.delete(portfolioId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
