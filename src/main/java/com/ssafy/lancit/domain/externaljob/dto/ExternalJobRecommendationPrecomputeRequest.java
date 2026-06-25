package com.ssafy.lancit.domain.externaljob.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalJobRecommendationPrecomputeRequest {
    private String jobCategory;
    private List<String> jobCategories;

    public List<String> resolveJobCategories() {
        List<String> candidates = new ArrayList<>();
        if (jobCategories != null) {
            candidates.addAll(jobCategories);
        }
        if (jobCategory != null) {
            candidates.add(jobCategory);
        }

        return candidates.stream()
                .map(ExternalJobRecommendationPrecomputeRequest::trimToNull)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
