package com.ssafy.lancit.domain.bookmark.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.bookmark.dto.BookmarkDTO;
import com.ssafy.lancit.domain.bookmark.service.BookmarkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {
 
    private final BookmarkService bookmarkService;
 
    /** CLI-SEAR-01 찜한 프리랜서 목록 조회 / 찜 필터 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookmarkDTO>>> getBookmarks() {
        // TODO 영은: bookmarkService.getList(companyEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-SEAR-01 직접 찜 (applicationId=null) */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createBookmark(@RequestBody BookmarkDTO dto) {
        // TODO 영은: bookmarkService.create(dto, companyEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** 찜 취소 */
    @DeleteMapping("/{bookmarkId}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(@PathVariable int bookmarkId) {
        // TODO 영은: bookmarkService.delete(bookmarkId, companyEmail)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** CLI-SEAR-01 프리랜서 검색 */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchFreelancers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String jobCategory) {
        // TODO 영은: bookmarkService.searchFreelancers(name, jobCategory, companyEmail)
        //   → User 목록 + 각 찜 여부 포함
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
