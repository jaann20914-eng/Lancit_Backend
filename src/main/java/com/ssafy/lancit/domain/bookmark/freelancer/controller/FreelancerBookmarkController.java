package com.ssafy.lancit.domain.bookmark.freelancer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.bookmark.freelancer.dto.RecruitmentBookmarkResponse;
import com.ssafy.lancit.domain.bookmark.freelancer.service.FreelancerBookmarkService;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentListItemResponse;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentSearchCondition;
import com.ssafy.lancit.domain.recruitment.post.service.RecruitmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recruitments")
@RequiredArgsConstructor
public class FreelancerBookmarkController {

    private final FreelancerBookmarkService freelancerBookmarkService;
    private final RecruitmentService recruitmentService;

    /**
     * 찜한 공고 목록 조회
     * GET /api/recruitments/bookmarked?page=1&size=10
     */
    @GetMapping("/bookmarked")
    public ResponseEntity<ApiResponse<PageResponse<RecruitmentListItemResponse>>> getBookmarkedList(
            @ModelAttribute RecruitmentSearchCondition condition,
            @ModelAttribute PageRequest pageRequest) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                recruitmentService.getBookmarkedList(email, role, condition, pageRequest)));
    }

    /**
     * 찜 토글 (추가 / 취소)
     * POST /api/recruitments/{recruitmentId}/bookmark
     */
    @PostMapping("/{recruitmentId}/bookmark")
    public ResponseEntity<ApiResponse<RecruitmentBookmarkResponse>> toggle(@PathVariable int recruitmentId) {
        String email = SecurityUtil.getCurrentEmail();
        String role = SecurityUtil.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.ok(
                freelancerBookmarkService.toggle(email, role, recruitmentId)));
    }
}
