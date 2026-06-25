package com.ssafy.lancit.domain.externaljob.controller;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCardResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDetailResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobImportResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobRecommendationPrecomputeRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobRecommendationPrecomputeResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobRecommendationRefreshRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobRecommendationRefreshResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobCollectService;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobCsvImportService;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobQueryService;
import com.ssafy.lancit.domain.externaljob.service.ExternalJobRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "External Jobs", description = "외부 공고 조회 및 수동 수집 API")
@RestController
@RequestMapping("/api/external-jobs")
@RequiredArgsConstructor
public class ExternalJobController {

    private final ExternalJobQueryService externalJobQueryService;
    private final ExternalJobCollectService externalJobCollectService;
    private final ExternalJobRecommendationService externalJobRecommendationService;
    private final ExternalJobCsvImportService externalJobCsvImportService;

    @Operation(
            summary = "외부 공고 목록 조회",
            description = "직종별 사전 추천 결과가 저장된 노출 가능 외부 공고를 조회합니다. NOT_FREELANCE 공고는 제외합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExternalJobCardResponse>>> getExternalJobs(
            @ModelAttribute ExternalJobSearchCondition condition,
            @ModelAttribute PageRequest pageRequest) {
        ExternalJobSearchCondition safeCondition = condition == null ? new ExternalJobSearchCondition() : condition;
        safeCondition.setUserEmail(SecurityUtil.getCurrentEmailOrNull());
        return ResponseEntity.ok(ApiResponse.ok(
                externalJobQueryService.listExternalJobs(safeCondition, pageRequest)));
    }

    @Operation(
            summary = "외부 공고 사전 추천 준비 상태 확인",
            description = "요청 직종 기준으로 DB에 저장된 사전 추천 결과가 있는지 확인합니다. 사용자 요청 중 Gemini/LLM을 호출하지 않습니다.")
    @PostMapping("/recommendations/refresh")
    public ResponseEntity<ApiResponse<ExternalJobRecommendationRefreshResponse>> refreshRecommendations(
            @RequestBody(required = false) ExternalJobRecommendationRefreshRequest request) {
        String jobCategory = request == null ? null : request.getJobCategory();
        return ResponseEntity.ok(ApiResponse.ok(
                externalJobRecommendationService.refreshPersonalRecommendations(null, jobCategory)));
    }

    @Operation(
            summary = "외부 공고 CSV import",
            description = "관리자/개발용으로 외부 공고 원본 CSV 또는 직종별 추천 결과 CSV를 DB에 적재합니다.")
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExternalJobImportResponse>> importCsv(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(externalJobCsvImportService.importCsv(file)));
    }

    @Operation(
            summary = "외부 공고 직종별 추천 사전 계산",
            description = "관리자/개발용으로 현재 DB의 노출 가능한 외부 공고에 대해 직종별 추천 결과를 미리 계산해 저장합니다.")
    @PostMapping("/recommendations/precompute")
    public ResponseEntity<ApiResponse<ExternalJobRecommendationPrecomputeResponse>> precomputeRecommendations(
            @RequestBody ExternalJobRecommendationPrecomputeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                externalJobRecommendationService.precomputeCategoryRecommendations(request)));
    }

    @Operation(
            summary = "외부 공고 상세 조회",
            description = "목록과 동일한 노출 정책을 적용하며, 원문 사이트 확인용 상세 정보를 반환합니다.")
    @GetMapping("/{externalJobId}")
    public ResponseEntity<ApiResponse<ExternalJobDetailResponse>> getExternalJob(
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

    @Operation(
            summary = "서울시 외부 공고 재분류",
            description = "기존 DB의 서울시 외부 공고를 현재 분류 정책으로 다시 분류하고 노출 여부를 갱신합니다.")
    @PostMapping("/reclassify/seoul")
    public ResponseEntity<ApiResponse<ExternalJobCollectResponse>> reclassifySeoulJobs() {
        return ResponseEntity.ok(ApiResponse.ok(externalJobCollectService.reclassifySeoulJobs()));
    }
}
