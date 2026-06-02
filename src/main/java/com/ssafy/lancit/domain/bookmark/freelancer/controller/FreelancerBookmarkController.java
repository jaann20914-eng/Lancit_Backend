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
import com.ssafy.lancit.domain.bookmark.freelancer.service.FreelancerBookmarkService;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/freelancer/bookmarks")
@RequiredArgsConstructor
public class FreelancerBookmarkController {

    private final FreelancerBookmarkService freelancerBookmarkService;

    /**
     * 찜한 공고 목록 조회
     * GET /api/freelancer/bookmarks?page=1&size=10
     */
    @GetMapping// 찜한 공고 목록 조회 (페이지네이션)
    public ResponseEntity<ApiResponse<PageResponse<RecruitmentDTO>>> getBookmarkedList(
            @ModelAttribute PageRequest pageRequest) {
        String email = SecurityUtil.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(
                freelancerBookmarkService.getBookmarkedList(email, pageRequest)));
    }

    /**
     * 찜 토글 (추가 / 취소)
     * POST /api/freelancer/bookmarks/{recruitmentId}
     */
    @PostMapping("/{recruitmentId}") // 찜 토글 - true=찜 추가, false=찜 취소
    public ResponseEntity<ApiResponse<Boolean>> toggle(@PathVariable int recruitmentId) {
        String email = SecurityUtil.getCurrentEmail();
        boolean result = freelancerBookmarkService.toggle(email, recruitmentId);
        String message = result ? "찜 추가" : "찜 취소";
        return ResponseEntity.ok(ApiResponse.ok(message, result));
    }

    /**
     * 특정 공고 찜 여부 확인
     * GET /api/freelancer/bookmarks/{recruitmentId}/status
     */
    @GetMapping("/{recruitmentId}/status")  // 특정 공고 찜 여부 확인
    public ResponseEntity<ApiResponse<Boolean>> isBookmarked(@PathVariable int recruitmentId) {
        String email = SecurityUtil.getCurrentEmail();
        return ResponseEntity.ok(ApiResponse.ok(
                freelancerBookmarkService.isBookmarked(email, recruitmentId)));
    }
}