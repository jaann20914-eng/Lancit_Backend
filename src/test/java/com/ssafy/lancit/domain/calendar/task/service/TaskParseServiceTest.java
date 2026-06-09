package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.global.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskParseServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final TaskParseService taskParseService = new TaskParseService();

    @Test
    void parseThrowsInvalidInputWhenSourceTextIsNull() {
        assertThatThrownBy(() -> taskParseService.parse(TaskParseRequestDTO.builder().sourceText(null).build()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void parseThrowsInvalidInputWhenSourceTextIsBlank() {
        assertThatThrownBy(() -> taskParseService.parse(TaskParseRequestDTO.builder().sourceText("   ").build()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    void parseUsesAiResultWhenAiClientSucceeds() {
        LocalDateTime aiStartAt = LocalDateTime.of(2026, 6, 10, 14, 0);
        TaskParseService service = new TaskParseService(sourceText -> TaskParseResponseDTO.builder()
                .sourceText(sourceText)
                .categoryId(null)
                .title("AI 파싱 일정")
                .content(sourceText)
                .memo(null)
                .startAt(aiStartAt)
                .endAt(aiStartAt.plusHours(1))
                .status(TaskStatus.IN_PROGRESS)
                .clientCompany("랜싯")
                .budget(1_000_000)
                .paidAt(null)
                .confidence(0.92)
                .warnings(List.of())
                .build());

        TaskParseResponseDTO result = parse(service, "내일 오후 3시에 삼성전자 미팅");

        assertThat(result.getTitle()).isEqualTo("AI 파싱 일정");
        assertThat(result.getStartAt()).isEqualTo(aiStartAt);
        assertThat(result.getClientCompany()).isEqualTo("랜싯");
        assertThat(result.getConfidence()).isEqualTo(0.92);
    }

    @Test
    void parseFallsBackToRulesWhenAiClientFails() {
        TaskParseService service = new TaskParseService(sourceText -> {
            throw new IllegalStateException("AI unavailable");
        });

        TaskParseResponseDTO result = parse(service, "내일 오후 3시에 삼성전자 미팅");

        assertThat(result.getTitle()).isEqualTo("삼성전자 미팅");
        assertThat(result.getStartAt()).isEqualTo(LocalDate.now(SEOUL_ZONE).plusDays(1).atTime(15, 0));
        assertThat(result.getClientCompany()).isEqualTo("삼성전자");
    }

    @Test
    void parseFallsBackToRulesWhenGmsKeyIsMissing() {
        TaskParseService service = new TaskParseService(new GmsGeminiTaskParseClient(new ObjectMapper()));

        TaskParseResponseDTO result = parse(service, "내일 오후 3시에 삼성전자 미팅");

        assertThat(result.getTitle()).isEqualTo("삼성전자 미팅");
        assertThat(result.getStartAt()).isEqualTo(LocalDate.now(SEOUL_ZONE).plusDays(1).atTime(15, 0));
        assertThat(result.getClientCompany()).isEqualTo("삼성전자");
    }

    @Test
    void parseRelativeDateTimeTitleAndClientCompany() {
        TaskParseResponseDTO result = parse("내일 오후 3시에 삼성전자 미팅");

        assertThat(result.getTitle()).isEqualTo("삼성전자 미팅");
        assertThat(result.getStartAt()).isEqualTo(LocalDate.now(SEOUL_ZONE).plusDays(1).atTime(15, 0));
        assertThat(result.getClientCompany()).isEqualTo("삼성전자");
        assertThat(result.getCategoryId()).isNull();
    }

    @Test
    void parseMeetingRoomAsMemoNotClientCompany() {
        String sourceText = "내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의";

        TaskParseResponseDTO result = parse(sourceText);

        assertThat(result.getSourceText()).isEqualTo(sourceText);
        assertThat(result.getStartAt()).isEqualTo(LocalDate.now(SEOUL_ZONE).plusDays(1).atTime(15, 0));
        assertThat(result.getTitle()).isEqualTo("팀 회의");
        assertThat(result.getClientCompany()).isNull();
        assertThat(result.getMemo()).isEqualTo("SSAFY 1층 회의실");
        assertThat(result.getContent()).isNotEqualTo(sourceText);
    }

    @Test
    void parseMonthDayTimeRangeAndTitle() {
        TaskParseResponseDTO result = parse("6월 12일 14:00~16:00 랜싯 프로젝트 회의");

        assertThat(result.getTitle()).isEqualTo("랜싯 프로젝트 회의");
        assertThat(result.getStartAt().toLocalDate().getMonthValue()).isEqualTo(6);
        assertThat(result.getStartAt().toLocalDate().getDayOfMonth()).isEqualTo(12);
        assertThat(result.getStartAt().toLocalTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(result.getEndAt().toLocalTime()).isEqualTo(LocalTime.of(16, 0));
    }

    @Test
    void parseBudgetInManwon() {
        TaskParseResponseDTO result = parse("예산 300만원 랜싯 외주 작업");

        assertThat(result.getTitle()).isEqualTo("랜싯 외주 작업");
        assertThat(result.getBudget()).isEqualTo(3_000_000);
    }

    @Test
    void parsePaidAtWithSettlementKeyword() {
        TaskParseResponseDTO result = parse("7월 1일 정산 300만원 입금 예정");

        assertThat(result.getBudget()).isEqualTo(3_000_000);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(result.getPaidAt().toLocalDate().getMonthValue()).isEqualTo(7);
        assertThat(result.getPaidAt().toLocalDate().getDayOfMonth()).isEqualTo(1);
        assertThat(result.getStartAt()).isNull();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("지급일"));
    }

    private TaskParseResponseDTO parse(String sourceText) {
        return parse(taskParseService, sourceText);
    }

    private TaskParseResponseDTO parse(TaskParseService service, String sourceText) {
        return service.parse(TaskParseRequestDTO.builder().sourceText(sourceText).build());
    }
}
