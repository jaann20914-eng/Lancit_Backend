package com.ssafy.lancit.domain.externaljob.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobPersonalRecommendation;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobRecommendationRefreshResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUserRecommendationCommand;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalJobRecommendationService {

    private static final int GEMINI_BATCH_SIZE = 25;
    private static final int CATEGORY_MATCH_BONUS = 12;
    private static final String MATCHED_BY_GEMINI = "GEMINI";
    private static final String MATCHED_BY_FALLBACK = "FALLBACK";

    private final ExternalJobMapper externalJobMapper;
    private final ObjectProvider<GeminiExternalJobPersonalRecommendationClient> geminiClientProvider;

    public ExternalJobRecommendationRefreshResponse refreshPersonalRecommendations(String userEmail, String jobCategory) {
        String safeUserEmail = trimToNull(userEmail);
        String safeJobCategory = trimToNull(jobCategory);
        if (safeUserEmail == null || safeJobCategory == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<ExternalJobDTO> jobs = externalJobMapper.findVisibleExternalJobsForRecommendation();
        int refreshedCount = 0;
        for (int start = 0; start < jobs.size(); start += GEMINI_BATCH_SIZE) {
            List<ExternalJobDTO> batch = jobs.subList(start, Math.min(start + GEMINI_BATCH_SIZE, jobs.size()));
            Map<Long, ExternalJobPersonalRecommendation> geminiRecommendations =
                    recommendWithGemini(safeJobCategory, batch);

            for (ExternalJobDTO job : batch) {
                if (job.getId() == null) {
                    continue;
                }
                ExternalJobPersonalRecommendation recommendation = geminiRecommendations.get(job.getId());
                if (recommendation == null) {
                    recommendation = fallbackRecommendation(job, safeJobCategory);
                }
                externalJobMapper.upsertExternalJobUserRecommendation(
                        toCommand(safeUserEmail, safeJobCategory, job, recommendation));
                refreshedCount++;
            }
        }

        return ExternalJobRecommendationRefreshResponse.builder()
                .jobCategory(safeJobCategory)
                .refreshedCount(refreshedCount)
                .build();
    }

    private Map<Long, ExternalJobPersonalRecommendation> recommendWithGemini(String jobCategory,
                                                                             List<ExternalJobDTO> batch) {
        GeminiExternalJobPersonalRecommendationClient geminiClient = geminiClientProvider.getIfAvailable();
        if (geminiClient == null) {
            return Map.of();
        }
        try {
            return geminiClient.recommend(jobCategory, batch).stream()
                    .filter(recommendation -> recommendation.getExternalJobId() != null)
                    .collect(Collectors.toMap(
                            ExternalJobPersonalRecommendation::getExternalJobId,
                            Function.identity(),
                            (first, second) -> first));
        } catch (RuntimeException e) {
            log.warn("Gemini external job personal recommendation failed. Falling back to global scores. reason={}",
                    e.getClass().getSimpleName());
            return Map.of();
        }
    }

    private ExternalJobPersonalRecommendation fallbackRecommendation(ExternalJobDTO job, String jobCategory) {
        int score = clamp(job.getRecommendationScore());
        if (matchesJobCategory(job, jobCategory)) {
            score = clamp(score + CATEGORY_MATCH_BONUS);
        }
        return ExternalJobPersonalRecommendation.builder()
                .externalJobId(job.getId())
                .recommendationType(recommendationTypeForScore(score))
                .recommendationScore(score)
                .matchedBy(MATCHED_BY_FALLBACK)
                .build();
    }

    private ExternalJobUserRecommendationCommand toCommand(String userEmail,
                                                           String jobCategory,
                                                           ExternalJobDTO job,
                                                           ExternalJobPersonalRecommendation recommendation) {
        int score = clamp(recommendation.getRecommendationScore());
        ExternalJobRecommendationType type = recommendation.getRecommendationType();
        if (type == null) {
            type = recommendationTypeForScore(score);
        }
        if (ExternalJobRecommendationType.EXCLUDED.equals(type)) {
            score = 0;
        }

        String matchedBy = trimToNull(recommendation.getMatchedBy());
        return ExternalJobUserRecommendationCommand.builder()
                .userEmail(userEmail)
                .externalJobId(job.getId())
                .jobCategory(jobCategory)
                .recommendationType(type)
                .recommendationScore(score)
                .matchedBy(MATCHED_BY_GEMINI.equals(matchedBy) ? MATCHED_BY_GEMINI : MATCHED_BY_FALLBACK)
                .build();
    }

    private boolean matchesJobCategory(ExternalJobDTO job, String jobCategory) {
        String normalizedCategory = normalize(jobCategory);
        if (normalizedCategory == null) {
            return false;
        }

        String text = normalize(String.join(" ",
                value(job.getTitle()),
                value(job.getJobCategoryRaw()),
                value(job.getDescription())));
        if (text == null) {
            return false;
        }
        if (text.contains(normalizedCategory)) {
            return true;
        }
        return Arrays.stream(normalizedCategory.split("[/\\s,|·]+"))
                .filter(token -> token.length() >= 2)
                .anyMatch(text::contains);
    }

    private static ExternalJobRecommendationType recommendationTypeForScore(int score) {
        if (score >= 90) {
            return ExternalJobRecommendationType.HIGHLY_RECOMMENDED;
        }
        if (score >= 65) {
            return ExternalJobRecommendationType.RECOMMENDED;
        }
        if (score >= 30) {
            return ExternalJobRecommendationType.POSSIBLE;
        }
        return ExternalJobRecommendationType.EXCLUDED;
    }

    private static int clamp(Integer score) {
        return score == null ? 0 : Math.max(0, Math.min(100, score));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
