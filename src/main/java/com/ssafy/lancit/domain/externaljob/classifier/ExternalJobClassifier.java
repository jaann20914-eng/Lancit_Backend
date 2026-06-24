package com.ssafy.lancit.domain.externaljob.classifier;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassification;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobClassificationInput;

public interface ExternalJobClassifier {
    ExternalJobClassification classify(ExternalJobClassificationInput input);
}
