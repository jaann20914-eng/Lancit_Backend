package com.ssafy.lancit.domain.calendar.task.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GmsGeminiTaskParseClientTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDate.of(2026, 6, 23).atStartOfDay(SEOUL_ZONE).toInstant(),
            SEOUL_ZONE
    );

    // 변경 전 9개 few-shot 포함 런타임 프롬프트는 같은 입력에서 약 17,950자였다.
    private static final int LEGACY_RUNTIME_PROMPT_LOWER_BOUND = 17_000;
    private static final List<String> DTO_FIELDS = List.of(
            "sourceText", "categoryId", "title", "content", "memo",
            "startAt", "startDate", "startTime", "startText", "startPrecision",
            "endAt", "endDate", "endTime", "endText", "endPrecision",
            "status", "clientCompany", "budget", "budgetAmount", "depositAmount",
            "paidAmount", "balanceAmount", "contractAmount", "budgetText",
            "paidAt", "paidDate", "paidTime", "paidText", "paidPrecision",
            "confidence", "warnings"
    );

    @Test
    void compactPromptKeepsDtoAndParsingContractsWithoutFewShotExamples() {
        GmsGeminiTaskParseClient client = new GmsGeminiTaskParseClient(
                new ObjectMapper(),
                new MockEnvironment(),
                FIXED_CLOCK
        );

        String prompt = client.createPrompt("프로젝트 검토 일정");

        assertThat(prompt)
                .contains("JSON 객체 하나만 반환")
                .contains("DTO 외 필드는 출력하지 않습니다")
                .contains("categoryId는 항상 null")
                .contains("status 기본값은 \"IN_PROGRESS\"")
                .contains("날짜만 있으면 *At/*Time=null")
                .contains("시간만 있으면 *At/*Date=null")
                .contains("지급/입금/정산 중심 문장은 paid*에만 반영")
                .contains("일정일과 지급일이 함께 있으면 start*와 paid*로 분리")
                .contains("장소, 회의실, 링크, 준비물, 회의 방식과 부가 설명은 memo")
                .contains("clientCompany에는 회사/거래처만")
                .contains("budgetAmount=전체 예산/견적")
                .contains("depositAmount=계약금/선금")
                .contains("paidAmount=실제 입금/지급액")
                .contains("balanceAmount=잔금")
                .contains("contractAmount=계약 총액")
                .contains("budgetText=숫자로 확정할 수 없는 범위/비율 원문")
                .doesNotContain(
                        "Few-shot 예시",
                        "7월 1일 오후 2시에 삼성전자와 계약 미팅",
                        "내일 오후 3시에 SSAFY 1층 회의실",
                        "6월 20일 오후 4시 온라인 회의"
                );
        DTO_FIELDS.forEach(field -> assertThat(prompt).contains("\"" + field + "\""));
    }

    @Test
    void compactPromptIsShorterThanLegacyRuntimePrompt() {
        GmsGeminiTaskParseClient client = new GmsGeminiTaskParseClient(
                new ObjectMapper(),
                new MockEnvironment(),
                FIXED_CLOCK
        );

        String prompt = client.createPrompt("프로젝트 검토 일정");

        assertThat(prompt.length())
                .as("축약 프롬프트 길이 (변경 전 약 17,950자)")
                .isLessThan(LEGACY_RUNTIME_PROMPT_LOWER_BOUND);
    }

    @Test
    void missingApiKeyStopsBeforeExternalRequestPath() {
        GmsGeminiTaskParseClient client = new GmsGeminiTaskParseClient(
                new ObjectMapper(),
                new MockEnvironment(),
                FIXED_CLOCK
        );

        assertThatThrownBy(() -> client.parse("프로젝트 검토 일정"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("GMS API key is not configured");
    }
}
