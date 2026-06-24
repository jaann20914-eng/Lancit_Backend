package com.ssafy.lancit.domain.bookmark.company.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;
import com.ssafy.lancit.domain.bookmark.company.dto.TalentListDTO;
import com.ssafy.lancit.domain.bookmark.company.service.CompanyBookmarkService;
import com.ssafy.lancit.domain.company.service.CompanyService;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.global.enums.JobCategory;

import lombok.RequiredArgsConstructor;

// 회사가 프리랜서 찜 (bookmark 테이블)
// Redis, STOMP 직접 관련 없음
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class CompanyBookmarkController {

    private final CompanyBookmarkService companyBookmarkService;
    private final CompanyService companyService;
    /**
     * CLI-SEAR-01 프리랜서 목록 조회 + 검색 + 필터 + 정렬 + 페이지네이션
     * - keyword: 이름 검색
     * - jobCategory: 업종 필터, 디폴트 값은 회사의 잡 카테고리
     * - bookmarked: true 면 찜한 프리랜서만
     * - sort: latest / name
     * - 각 프리랜서마다 isBookmarked 포함 반환
     */
//    @GetMapping("/search")
//    public ResponseEntity<ApiResponse<?>> searchFreelancers(
//            @RequestParam(required = false) String keyword,
//            @RequestParam(required = false) JobCategory jobCategory,
//            @RequestParam(defaultValue = "false") boolean bookmarked,
//            @ModelAttribute PageRequest pageRequest) {
//
//        String companyEmail = SecurityUtil.getCurrentEmail();
//        
//        PageResponse<UserDTO> result = companyBookmarkService.searchFreelancers(
//                companyEmail, keyword, jobCategory, bookmarked, pageRequest);
//        return ResponseEntity.ok(ApiResponse.ok(result));
//    }
    
    

    // CLI-SEAR-01 프리랜서 찜하기

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createBookmark(
            @RequestBody CompanyBookmarkDTO dto) {
        String companyEmail = SecurityUtil.getCurrentEmail();
        companyBookmarkService.create(dto, companyEmail);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-SEAR-01 프리랜서 찜 취소
//    @DeleteMapping("/{bookmarkId}")
//    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
//            @PathVariable int bookmarkId) {
//        String companyEmail = SecurityUtil.getCurrentEmail();
//        companyBookmarkService.delete(bookmarkId, companyEmail);
//        return ResponseEntity.ok(ApiResponse.ok(null));
//    }
    
 // CompanyBookmarkController.java에 추가
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteBookmarkByFreelancer(
            @RequestParam String freelancerEmail) {
        String companyEmail = SecurityUtil.getCurrentEmail();
        companyBookmarkService.deleteByFreelancer(companyEmail, freelancerEmail);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchTalents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobCategory jobCategory,
            @RequestParam(defaultValue = "false") boolean bookmarked,
            @ModelAttribute PageRequest pageRequest) {

        String companyEmail = SecurityUtil.getCurrentEmail();

        PageResponse<TalentListDTO> result =
                companyBookmarkService.searchTalents(
                        companyEmail,
                        keyword,
                        jobCategory,
                        bookmarked,
                        pageRequest);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
}
