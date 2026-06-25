package com.ssafy.lancit.domain.externaljob.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCardResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDetailResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalJobQueryService {

    private static final String DETAIL_BUTTON_LABEL = "상세 보기";
    private static final String EXTERNAL_NOTICE = "외부 공고는 원문 사이트에서 상세 내용을 확인하고 지원을 진행해주세요.";
    private static final int SUMMARY_MAX_LENGTH = 120;

    private final ExternalJobMapper externalJobMapper;

    public PageResponse<ExternalJobCardResponse> listExternalJobs(ExternalJobSearchCondition condition,
                                                                  PageRequest pageRequest) {
        ExternalJobSearchCondition normalized = normalize(condition);
        PageRequest safePageRequest = pageRequest == null ? new PageRequest() : pageRequest;
        List<ExternalJobCardResponse> content = externalJobMapper.findExternalJobs(normalized, safePageRequest)
                .stream()
                .map(this::toCardResponse)
                .toList();
        long total = externalJobMapper.countExternalJobs(normalized);
        return PageResponse.of(content, total, safePageRequest);
    }

    public ExternalJobDetailResponse getExternalJob(Long id) {
        if (id == null || id <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        ExternalJobDTO dto = externalJobMapper.findById(id);
        if (dto == null) {
            throw new CustomException(ErrorCode.EXTERNAL_JOB_NOT_FOUND);
        }
        return toDetailResponse(dto);
    }

    private ExternalJobSearchCondition normalize(ExternalJobSearchCondition condition) {
        ExternalJobSearchCondition normalized = condition == null ? new ExternalJobSearchCondition() : condition;
        normalized.setKeyword(trimToNull(normalized.getKeyword()));
        normalized.setJobCategory(trimToNull(normalized.getJobCategory()));
        normalized.setUserEmail(trimToNull(normalized.getUserEmail()));
        normalized.setSort(ExternalJobSort.RECOMMENDED);
        return normalized;
    }

    private ExternalJobCardResponse toCardResponse(ExternalJobDTO dto) {
        ExternalJobRecommendationType recommendationType = dto.getRecommendationType();
        return ExternalJobCardResponse.builder()
                .id(dto.getId())
                .externalJobId(dto.getId())
                .source(dto.getSource())
                .sourceLabel(dto.getSource() == null ? null : dto.getSource().getLabel())
                .title(dto.getTitle())
                .companyName(dto.getCompanyName())
                .location(dto.getLocation())
                .workLocation(dto.getLocation())
                .jobCategoryRaw(dto.getJobCategoryRaw())
                .employmentTypeRaw(dto.getEmploymentTypeRaw())
                .salaryText(dto.getSalaryRaw())
                .deadlineAt(dto.getDeadlineAt())
                .detailButtonLabel(DETAIL_BUTTON_LABEL)
                .sourceUrl(dto.getSourceUrl())
                .sourceButtonLabel(dto.getSource() == null ? null : dto.getSource().getButtonLabel())
                .freelanceType(dto.getFreelanceType())
                .recommendationType(recommendationType)
                .recommendationLabel(recommendationType == null ? null : recommendationType.getLabel())
                .collectedAt(dto.getCollectedAt())
                .build();
    }

    private ExternalJobDetailResponse toDetailResponse(ExternalJobDTO dto) {
        ExternalJobRecommendationType recommendationType = dto.getRecommendationType();
        String content = trimToNull(dto.getDescription());
        LocalDateTime createdAt = dto.getPostedAt() == null ? dto.getCollectedAt() : dto.getPostedAt();

        return ExternalJobDetailResponse.builder()
                .id(dto.getId())
                .externalJobId(dto.getId())
                .source(dto.getSource())
                .sourceLabel(dto.getSource() == null ? null : dto.getSource().getLabel())
                .sourceJobId(dto.getSourceJobId())
                .title(dto.getTitle())
                .summary(resolveSummary(dto, content))
                .content(content)
                .description(content)
                .requirements(resolveRequirements(dto))
                .companyName(dto.getCompanyName())
                .location(dto.getLocation())
                .workLocation(dto.getLocation())
                .jobCategoryRaw(dto.getJobCategoryRaw())
                .employmentTypeRaw(dto.getEmploymentTypeRaw())
                .salaryText(dto.getSalaryRaw())
                .postedAt(dto.getPostedAt())
                .deadlineAt(dto.getDeadlineAt())
                .recruitmentStartAt(dto.getPostedAt())
                .recruitmentEndAt(dto.getDeadlineAt())
                .createdAt(createdAt)
                .collectedAt(dto.getCollectedAt())
                .updatedAt(dto.getUpdatedAt())
                .detailButtonLabel(DETAIL_BUTTON_LABEL)
                .sourceUrl(dto.getSourceUrl())
                .sourceButtonLabel(dto.getSource() == null ? null : dto.getSource().getButtonLabel())
                .freelanceType(dto.getFreelanceType())
                .recommendationType(recommendationType)
                .recommendationLabel(recommendationType == null ? null : recommendationType.getLabel())
                .applicantCount(0)
                .canApply(false)
                .isApplied(false)
                .isBookmarked(false)
                .externalNotice(EXTERNAL_NOTICE)
                .build();
    }

    private String resolveSummary(ExternalJobDTO dto, String content) {
        if (content != null) {
            return abbreviate(content.replaceAll("\\s+", " "), SUMMARY_MAX_LENGTH);
        }
        return combineNonBlank(dto.getJobCategoryRaw(), dto.getEmploymentTypeRaw(), dto.getSalaryRaw());
    }

    private String resolveRequirements(ExternalJobDTO dto) {
        return combineNonBlank(dto.getJobCategoryRaw(), dto.getEmploymentTypeRaw());
    }

    private String combineNonBlank(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(trimmed);
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3).trim() + "...";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
