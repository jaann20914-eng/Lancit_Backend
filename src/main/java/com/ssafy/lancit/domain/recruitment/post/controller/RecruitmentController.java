package com.ssafy.lancit.domain.recruitment.post.controller;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.service.RecruitmentService;
import com.ssafy.lancit.global.enums.JobCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 공고문 CRUD - 프리랜서(조회), 회사(등록/조회)
// Redis, STOMP 직접 관련 없음
@RestController
@RequestMapping("/api/recruitments")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    // APPLY-01 프리랜서용 공고문 목록 조회 (페이지네이션, 업종/키워드 필터)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecruitmentDTO>>> getRecruitments(
            @RequestParam(required = false) JobCategory jobCategory,
            @RequestParam(required = false) String keyword,
            @ModelAttribute PageRequest pageRequest) {
        // TODO 영은 [1]: return ResponseEntity.ok(ApiResponse.ok(
        //               recruitmentService.getList(jobCategory, keyword, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-APPLY-01 회사 내 공고문 목록 조회 (페이지네이션, 상태 필터)
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<RecruitmentDTO>>> getMyRecruitments(
            @RequestParam(required = false) String status,
            @ModelAttribute PageRequest pageRequest) {
        // TODO 영은 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: return ResponseEntity.ok(ApiResponse.ok(
        //               recruitmentService.getMyList(email, status, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // APPLY-02 / CLI-APPLY-03 공고문 상세 조회
    @GetMapping("/{recruitmentId}")
    public ResponseEntity<ApiResponse<RecruitmentDTO>> getRecruitment(
            @PathVariable int recruitmentId) {
        // TODO 영은 [1]: return ResponseEntity.ok(ApiResponse.ok(
        //               recruitmentService.getOne(recruitmentId)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-APPLY-02 공고문 등록 (회사 전용)
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createRecruitment(@RequestBody RecruitmentDTO dto) {
        // TODO 영은 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 영은 [2]: recruitmentService.create(dto, email)
        // TODO 영은 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}