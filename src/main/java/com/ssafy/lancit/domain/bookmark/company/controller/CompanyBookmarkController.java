package com.ssafy.lancit.domain.bookmark.company.controller;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;
import com.ssafy.lancit.domain.bookmark.company.service.CompanyBookmarkService;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 회사가 프리랜서 찜 (bookmark 테이블)
// Redis, STOMP 직접 관련 없음
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class CompanyBookmarkController {

    private final CompanyBookmarkService bookmarkService;

    // CLI-SEAR-01 찜한 프리랜서 목록 조회 (페이지네이션)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CompanyBookmarkDTO>>> getBookmarks(
            @ModelAttribute PageRequest pageRequest) {
        // TODO 지원 [1]: String companyEmail = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: return ResponseEntity.ok(ApiResponse.ok(
        //               bookmarkService.getList(companyEmail, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-SEAR-01 직접 찜 추가
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createBookmark(@RequestBody CompanyBookmarkDTO dto) {
        // TODO 지원 [1]: String companyEmail = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: bookmarkService.create(dto, companyEmail)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 찜 취소
    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(@PathVariable int bookmarkId) {
        // TODO 지원 [1]: String companyEmail = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: bookmarkService.delete(bookmarkId, companyEmail)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-SEAR-01 프리랜서 검색 (이름/업종 필터 + 페이지네이션)
    // 검색 결과에 찜 여부 포함 → UserDTO 에 isBookmarked 필드 추가 or Map 으로 반환
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserDTO>>> searchFreelancers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String jobCategory,
            @ModelAttribute PageRequest pageRequest) {
        // TODO 지원 [1]: String companyEmail = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: return ResponseEntity.ok(ApiResponse.ok(
        //               bookmarkService.searchFreelancers(name, jobCategory, companyEmail, pageRequest)))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}