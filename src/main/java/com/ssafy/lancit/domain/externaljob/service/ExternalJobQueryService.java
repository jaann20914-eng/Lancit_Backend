package com.ssafy.lancit.domain.externaljob.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCardResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalJobQueryService {

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

    public ExternalJobCardResponse getExternalJob(Long id) {
        if (id == null || id <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        ExternalJobDTO dto = externalJobMapper.findById(id);
        if (dto == null) {
            throw new CustomException(ErrorCode.EXTERNAL_JOB_NOT_FOUND);
        }
        return toCardResponse(dto);
    }

    private ExternalJobSearchCondition normalize(ExternalJobSearchCondition condition) {
        ExternalJobSearchCondition normalized = condition == null ? new ExternalJobSearchCondition() : condition;
        normalized.setKeyword(trimToNull(normalized.getKeyword()));
        if (normalized.getSort() == null) {
            normalized.setSort(ExternalJobSort.LATEST);
        }
        if (normalized.getIncludeExpired() == null) {
            normalized.setIncludeExpired(false);
        }
        return normalized;
    }

    private ExternalJobCardResponse toCardResponse(ExternalJobDTO dto) {
        ExternalJobRecommendationType recommendationType = dto.getRecommendationType();
        return ExternalJobCardResponse.builder()
                .id(dto.getId())
                .source(dto.getSource())
                .sourceLabel(dto.getSource() == null ? null : dto.getSource().getLabel())
                .title(dto.getTitle())
                .companyName(dto.getCompanyName())
                .location(dto.getLocation())
                .jobCategoryRaw(dto.getJobCategoryRaw())
                .employmentTypeRaw(dto.getEmploymentTypeRaw())
                .salaryText(dto.getSalaryRaw())
                .deadlineAt(dto.getDeadlineAt())
                .sourceUrl(dto.getSourceUrl())
                .freelanceType(dto.getFreelanceType())
                .recommendationType(recommendationType)
                .recommendationLabel(recommendationType == null ? null : recommendationType.getLabel())
                .collectedAt(dto.getCollectedAt())
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
