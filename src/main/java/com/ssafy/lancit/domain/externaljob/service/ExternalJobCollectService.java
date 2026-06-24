package com.ssafy.lancit.domain.externaljob.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.lancit.domain.externaljob.classifier.ExternalJobClassifier;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassificationInput;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResponse;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResult;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.domain.externaljob.mapper.ExternalJobMapper;
import com.ssafy.lancit.domain.externaljob.normalizer.SeoulExternalJobNormalizer;
import com.ssafy.lancit.domain.externaljob.provider.ExternalJobProvider;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ExternalJobCollectService {

    private final List<ExternalJobProvider> providers;
    private final SeoulExternalJobNormalizer seoulExternalJobNormalizer;
    private final ExternalJobClassifier externalJobClassifier;
    private final ExternalJobMapper externalJobMapper;

    public ExternalJobCollectService(List<ExternalJobProvider> providers,
                                     SeoulExternalJobNormalizer seoulExternalJobNormalizer,
                                     ExternalJobClassifier externalJobClassifier,
                                     ExternalJobMapper externalJobMapper) {
        this.providers = providers == null ? List.of() : providers;
        this.seoulExternalJobNormalizer = seoulExternalJobNormalizer;
        this.externalJobClassifier = externalJobClassifier;
        this.externalJobMapper = externalJobMapper;
    }

    @Transactional
    public ExternalJobCollectResponse collectSeoulJobs(ExternalJobCollectCommand command) {
        ExternalJobProvider provider = findProvider(ExternalJobSource.SEOUL);
        ExternalJobCollectResult providerResult = provider.collect(command);

        int upsertedCount = 0;
        int skippedCount = providerResult.getSkippedCount();
        int failedCount = providerResult.getFailedCount();

        for (JsonNode row : providerResult.safeRawRows()) {
            try {
                ExternalJobUpsertCommand normalized = seoulExternalJobNormalizer.normalize(row).orElse(null);
                if (normalized == null) {
                    skippedCount++;
                    continue;
                }

                ExternalJobUpsertCommand classified = applyClassification(normalized);
                if (externalJobMapper.upsertExternalJob(classified) > 0) {
                    upsertedCount++;
                }
            } catch (RuntimeException e) {
                failedCount++;
                log.warn("Failed to process Seoul external job row.", e);
            }
        }

        return ExternalJobCollectResponse.builder()
                .source(ExternalJobSource.SEOUL)
                .fetchedCount(providerResult.getFetchedCount())
                .upsertedCount(upsertedCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .message(resolveMessage(providerResult.getMessage(), upsertedCount, skippedCount, failedCount))
                .build();
    }

    private ExternalJobUpsertCommand applyClassification(ExternalJobUpsertCommand command) {
        ExternalJobClassification classification = externalJobClassifier.classify(toClassificationInput(command));
        ExternalFreelanceType freelanceType = classification == null || classification.getFreelanceType() == null
                ? ExternalFreelanceType.UNKNOWN
                : classification.getFreelanceType();
        ExternalJobRecommendationType recommendationType =
                classification == null || classification.getRecommendationType() == null
                        ? defaultRecommendation(freelanceType)
                        : classification.getRecommendationType();

        return command.toBuilder()
                .freelanceType(freelanceType)
                .recommendationType(recommendationType)
                .build();
    }

    private ExternalJobClassificationInput toClassificationInput(ExternalJobUpsertCommand command) {
        return ExternalJobClassificationInput.builder()
                .title(command.getTitle())
                .companyName(command.getCompanyName())
                .description(command.getDescription())
                .jobCategoryRaw(command.getJobCategoryRaw())
                .employmentTypeRaw(command.getEmploymentTypeRaw())
                .location(command.getLocation())
                .salaryRaw(command.getSalaryRaw())
                .build();
    }

    private ExternalJobProvider findProvider(ExternalJobSource source) {
        return providers.stream()
                .filter(provider -> source.equals(provider.getSource()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("External job provider not found: " + source));
    }

    private ExternalJobRecommendationType defaultRecommendation(ExternalFreelanceType freelanceType) {
        return switch (freelanceType) {
            case TRUE_FREELANCE -> ExternalJobRecommendationType.RECOMMENDED;
            case PROJECT_LIKE -> ExternalJobRecommendationType.RECOMMENDED;
            case CONTRACT_LIKE -> ExternalJobRecommendationType.POSSIBLE;
            case NOT_FREELANCE -> ExternalJobRecommendationType.EXCLUDED;
            case UNKNOWN -> ExternalJobRecommendationType.POSSIBLE;
        };
    }

    private String resolveMessage(String providerMessage, int upsertedCount, int skippedCount, int failedCount) {
        return "%s / upserted=%d, skipped=%d, failed=%d"
                .formatted(providerMessage == null ? "서울시 공고 수집 완료" : providerMessage,
                        upsertedCount, skippedCount, failedCount);
    }
}
