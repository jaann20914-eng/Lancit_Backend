package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

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
    void parseRelativeDateTimeTitleAndClientCompany() {
        TaskParseResponseDTO result = parse("내일 오후 3시에 삼성전자 미팅");

        assertThat(result.getTitle()).isEqualTo("삼성전자 미팅");
        assertThat(result.getStartAt()).isEqualTo(LocalDate.now(SEOUL_ZONE).plusDays(1).atTime(15, 0));
        assertThat(result.getClientCompany()).isEqualTo("삼성전자");
        assertThat(result.getCategoryId()).isNull();
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
        return taskParseService.parse(TaskParseRequestDTO.builder().sourceText(sourceText).build());
    }
}
