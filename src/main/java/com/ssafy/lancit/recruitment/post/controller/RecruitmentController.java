package com.ssafy.lancit.recruitment.post.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.recruitment.post.service.RecruitmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recruitments")
@RequiredArgsConstructor
public class RecruitmentController {
 
    private final RecruitmentService recruitmentService;
 
    /** APPLY-01 프리랜서용 공고문 목록 조회 (전체/업종 필터/검색) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecruitmentDTO>>> getRecruitments(
            @RequestParam(required = false) JobCategory jobCategory,
            @RequestParam(required = false) String keyword) {
        // TODO 영은: recruitmentService.getList(jobCategory, keyword)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-APPLY-01 회사 내 공고문 목록 조회 */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<RecruitmentDTO>>> getMyRecruitments(
            @RequestParam(required = false) String status) {
        // TODO 영은: recruitmentService.getMyList(email, status)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** APPLY-02 / CLI-APPLY-03 공고문 상세 조회 */
    @GetMapping("/{recruitmentId}")
    public ResponseEntity<ApiResponse<RecruitmentDTO>> getRecruitment(
            @PathVariable int recruitmentId) {
        // TODO 영은: recruitmentService.getOne(recruitmentId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-APPLY-02 공고문 등록 (회사 전용) */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createRecruitment(@RequestBody RecruitmentDTO dto) {
        // TODO 영은: recruitmentService.create(dto, email)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
