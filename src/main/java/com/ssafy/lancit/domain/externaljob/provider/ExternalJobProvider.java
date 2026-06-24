package com.ssafy.lancit.domain.externaljob.provider;

import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectResult;
import com.ssafy.lancit.global.enums.ExternalJobSource;

public interface ExternalJobProvider {
    ExternalJobSource getSource();

    ExternalJobCollectResult collect(ExternalJobCollectCommand command);
}
