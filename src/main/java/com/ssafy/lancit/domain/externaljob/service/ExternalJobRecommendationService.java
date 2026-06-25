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
    private static final List<String> IT_POSITIVE_KEYWORDS = List.of(
            "IT", "it", "개발", "웹", "앱", "소프트웨어", "서버", "프론트엔드", "백엔드",
            "데이터", "DB", "클라우드", "Java", "Spring", "React", "Vue", "Python"
    );
    private static final List<String> IT_HARD_NEGATIVE_KEYWORDS = List.of(
            "사무보조", "경리", "회계", "운전", "수행비서", "주차관리", "생산", "제조",
            "조작원", "물류", "배송", "청소", "경비"
    );

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
                .recommendationType(job.getRecommendationType() == null
                        ? recommendationTypeForScore(score)
                        : job.getRecommendationType())
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
        if (shouldPromoteExcludedItCandidate(jobCategory, job, type)) {
            type = ExternalJobRecommendationType.POSSIBLE;
            score = Math.max(score, 40);
        }
        score = normalizeRecommendationScore(score, type);

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
        String text = searchableJobText(job);
        if (text == null) {
            return false;
        }

        return keywordsFor(jobCategory).stream()
                .map(ExternalJobRecommendationService::normalize)
                .filter(keyword -> keyword != null)
                .flatMap(keyword -> Arrays.stream(keyword.split("[/\\s,|·]+")))
                .filter(ExternalJobRecommendationService::isSearchableToken)
                .anyMatch(text::contains);
    }

    private boolean shouldPromoteExcludedItCandidate(String jobCategory,
                                                     ExternalJobDTO job,
                                                     ExternalJobRecommendationType type) {
        if (!ExternalJobRecommendationType.EXCLUDED.equals(type) || !isItJobCategory(jobCategory)) {
            return false;
        }

        String text = searchableJobText(job);
        return text != null
                && containsAnyKeyword(text, IT_POSITIVE_KEYWORDS)
                && !containsAnyKeyword(text, IT_HARD_NEGATIVE_KEYWORDS);
    }

    private boolean isItJobCategory(String jobCategory) {
        String normalizedCategory = normalize(jobCategory);
        if (normalizedCategory == null) {
            return false;
        }
        return Arrays.stream(normalizedCategory.split("[/\\s,|·]+"))
                .anyMatch(token -> "it".equals(token) || "개발".equals(token));
    }

    private boolean containsAnyKeyword(String text, List<String> keywords) {
        return keywords.stream()
                .map(ExternalJobRecommendationService::normalize)
                .filter(keyword -> keyword != null)
                .anyMatch(text::contains);
    }

    private String searchableJobText(ExternalJobDTO job) {
        return normalize(String.join(" ",
                value(job.getTitle()),
                value(job.getJobCategoryRaw()),
                value(job.getDescription())));
    }

    private List<String> keywordsFor(String jobCategory) {
        String normalizedCategory = normalize(jobCategory);
        if (normalizedCategory == null) {
            return List.of();
        }

        return Arrays.stream(normalizedCategory.split("[/\\s,|·]+"))
                .filter(ExternalJobRecommendationService::isSearchableToken)
                .flatMap(token -> baseKeywordsFor(token).stream())
                .distinct()
                .toList();
    }

    private List<String> baseKeywordsFor(String token) {
        return switch (token.toUpperCase(Locale.ROOT)) {
            case "IT" -> List.of(
                    "개발", "웹", "앱", "소프트웨어", "프론트엔드", "백엔드",
                    "서버", "자바", "java", "spring", "react", "vue", "it", "데이터"
            );
            case "DESIGN" -> List.of(
                    "디자인", "디자이너", "ui", "ux", "웹디자인", "콘텐츠", "그래픽", "편집"
            );
            case "MARKETING" -> List.of(
                    "마케팅", "홍보", "광고", "sns", "콘텐츠", "브랜딩", "퍼포먼스"
            );
            case "VIDEO" -> List.of(
                    "영상", "촬영", "편집", "모션", "유튜브", "콘텐츠"
            );
            case "WRITING" -> List.of(
                    "글쓰기", "작가", "콘텐츠", "카피", "에디터", "기획", "문서"
            );
            case "EDUCATION" -> List.of(
                    "교육", "강사", "강의", "튜터", "멘토", "학습"
            );
            case "MUSIC" -> List.of(
                    "음악", "작곡", "편곡", "사운드", "오디오"
            );
            default -> List.of(token);
        };
    }

    private static boolean isSearchableToken(String token) {
        return token.length() >= 2 || "웹".equals(token) || "앱".equals(token);
    }

    private static ExternalJobRecommendationType recommendationTypeForScore(int score) {
        if (score >= 80) {
            return ExternalJobRecommendationType.HIGHLY_RECOMMENDED;
        }
        if (score >= 60) {
            return ExternalJobRecommendationType.RECOMMENDED;
        }
        if (score >= 40) {
            return ExternalJobRecommendationType.POSSIBLE;
        }
        return ExternalJobRecommendationType.EXCLUDED;
    }

    private static int normalizeRecommendationScore(int score, ExternalJobRecommendationType type) {
        return switch (type) {
            case HIGHLY_RECOMMENDED -> Math.max(80, score);
            case RECOMMENDED -> Math.max(60, Math.min(79, score));
            case POSSIBLE -> Math.max(40, Math.min(59, score));
            case EXCLUDED -> Math.min(39, score);
        };
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
