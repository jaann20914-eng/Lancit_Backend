package com.ssafy.lancit.domain.talent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.talent.dto.TalentDetailDTO;
import com.ssafy.lancit.domain.talent.dto.TalentListDTO;
import com.ssafy.lancit.domain.talent.dto.TalentSearchCondition;
import com.ssafy.lancit.domain.talent.service.TalentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/talents")
@RequiredArgsConstructor
@Tag(name = "Talent", description = "인재 찾기 API")
public class TalentController {

    private final TalentService talentService;

    @Operation(summary = "인재 찾기 목록 조회", description = "회사 계정만 접근할 수 있습니다. 공개 포트폴리오 프로필만 조회하며 VIEW_COUNT 또는 NAME 정렬을 지원합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TalentListDTO>>> getTalents(
            @ModelAttribute TalentSearchCondition condition) {
        return ResponseEntity.ok(ApiResponse.ok(talentService.getTalents(condition)));
    }

    @Operation(summary = "인재 상세 조회", description = "회사 계정만 접근할 수 있습니다. 상세 조회 시 조회수가 1 증가하며 공개 프로젝트만 포함합니다.")
    @GetMapping("/{freelancerEmail}")
    public ResponseEntity<ApiResponse<TalentDetailDTO>> getTalentDetail(
            @PathVariable String freelancerEmail) {
        return ResponseEntity.ok(ApiResponse.ok(talentService.getTalentDetail(freelancerEmail)));
    }

    @Operation(summary = "인재 찜 등록", description = "회사 계정만 사용할 수 있습니다. 이미 찜한 인재를 다시 찜해도 성공 처리합니다.")
    @PostMapping("/{freelancerEmail}/favorite")
    public ResponseEntity<ApiResponse<Void>> favorite(@PathVariable String freelancerEmail) {
        talentService.favorite(freelancerEmail);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "인재 찜 해제", description = "회사 계정만 사용할 수 있습니다. 찜하지 않은 인재를 해제해도 성공 처리합니다.")
    @DeleteMapping("/{freelancerEmail}/favorite")
    public ResponseEntity<ApiResponse<Void>> unfavorite(@PathVariable String freelancerEmail) {
        talentService.unfavorite(freelancerEmail);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
