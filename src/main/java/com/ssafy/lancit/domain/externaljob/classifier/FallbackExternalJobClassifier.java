package com.ssafy.lancit.domain.externaljob.classifier;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassificationInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class FallbackExternalJobClassifier implements ExternalJobClassifier {

    private final ObjectProvider<GeminiExternalJobClassifier> geminiClassifierProvider;
    private final RuleBasedExternalJobClassifier ruleBasedExternalJobClassifier;

    @Override
    public ExternalJobClassification classify(ExternalJobClassificationInput input) {
        GeminiExternalJobClassifier geminiClassifier = geminiClassifierProvider.getIfAvailable();
        if (geminiClassifier != null) {
            try {
                return geminiClassifier.classify(input);
            } catch (RuntimeException e) {
                log.warn("Gemini external job classification failed. Falling back to rules. reason={}",
                        e.getClass().getSimpleName());
            }
        }
        return ruleBasedExternalJobClassifier.classify(input);
    }
}
