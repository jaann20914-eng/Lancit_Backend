package com.ssafy.lancit.domain.externaljob.controller;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCardResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobCollectService;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "External Jobs", description = "외부 공고 조회 및 수동 수집 API")
@RestController
@RequestMapping("/api/external-jobs")
@RequiredArgsConstructor
public class ExternalJobController {

    private final ExternalJobQueryService externalJobQueryService;
    private final ExternalJobCollectService externalJobCollectService;

    @Operation(
            summary = "외부 공고 목록 조회",
            description = "서울시 등 외부 채용 공고를 조회합니다. 기본 조회에서는 NOT_FREELANCE, EXCLUDED, 마감 지난 공고를 제외합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExternalJobCardResponse>>> getExternalJobs(
            @ModelAttribute ExternalJobSearchCondition condition,
            @ModelAttribute PageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.ok(
                externalJobQueryService.listExternalJobs(condition, pageRequest)));
    }

    @Operation(
            summary = "외부 공고 상세 조회",
            description = "기본 노출 정책과 동일하게 제외/만료 공고는 조회하지 않습니다.")
    @GetMapping("/{externalJobId}")
    public ResponseEntity<ApiResponse<ExternalJobCardResponse>> getExternalJob(
            @PathVariable Long externalJobId) {
        return ResponseEntity.ok(ApiResponse.ok(externalJobQueryService.getExternalJob(externalJobId)));
    }

    @Operation(
            summary = "서울시 외부 공고 수동 수집",
            description = "1차 MVP 개발/검증용 수동 수집 API입니다. page/size 또는 startIndex/endIndex로 서울시 API 범위를 지정할 수 있습니다.")
    @PostMapping("/collect/seoul")
    public ResponseEntity<ApiResponse<ExternalJobCollectResponse>> collectSeoulJobs(
            @RequestBody(required = false) ExternalJobCollectRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                externalJobCollectService.collectSeoulJobs(ExternalJobCollectCommand.from(request))));
    }
}
