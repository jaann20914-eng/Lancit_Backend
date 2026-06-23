package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.global.enums.DateTimePrecision;
import com.ssafy.lancit.global.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskParseServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 6, 22);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_TODAY.atStartOfDay(SEOUL_ZONE).toInstant(),
            SEOUL_ZONE
    );

    private final TaskParseService taskParseService = new TaskParseService(null, FIXED_CLOCK);

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
                .build(), FIXED_CLOCK);

        TaskParseResponseDTO result = parse(service, "내일 오후 3시에 삼성전자 미팅");

        assertThat(result.getTitle()).isEqualTo("AI 파싱 일정");
        assertThat(result.getStartAt()).isEqualTo(aiStartAt);
        assertThat(result.getStartDate()).isEqualTo(aiStartAt.toLocalDate());
        assertThat(result.getStartTime()).isEqualTo(aiStartAt.toLocalTime());
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        assertThat(result.getClientCompany()).isEqualTo("랜싯");
        assertThat(result.getConfidence()).isEqualTo(0.92);
    }

    @Test
    void parseFallsBackToRulesWhenAiClientFails() {
        TaskParseService service = new TaskParseService(sourceText -> {
            throw new IllegalStateException("AI unavailable");
        }, FIXED_CLOCK);

        TaskParseResponseDTO result = parse(service, "내일 오후 3시에 삼성전자 미팅");

        assertThat(result.getTitle()).isEqualTo("삼성전자 미팅");
        assertThat(result.getStartAt()).isEqualTo(FIXED_TODAY.plusDays(1).atTime(15, 0));
        assertThat(result.getStartDate()).isEqualTo(FIXED_TODAY.plusDays(1));
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        assertThat(result.getClientCompany()).isEqualTo("삼성전자");
    }

    @Test
    void parseFallsBackToRulesWhenGmsKeyIsMissing() {
        TaskParseService service = new TaskParseService(new GmsGeminiTaskParseClient(new ObjectMapper(), FIXED_CLOCK), FIXED_CLOCK);

        TaskParseResponseDTO result = parse(service, "내일 오후 3시에 삼성전자 미팅");

        assertThat(result.getTitle()).isEqualTo("삼성전자 미팅");
        assertThat(result.getStartAt()).isEqualTo(FIXED_TODAY.plusDays(1).atTime(15, 0));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        assertThat(result.getClientCompany()).isEqualTo("삼성전자");
    }

    @Test
    void parseRelativeDateTimeTitleAndClientCompany() {
        TaskParseResponseDTO result = parse("내일 오후 3시에 삼성전자 미팅");

        assertThat(result.getTitle()).isEqualTo("삼성전자 미팅");
        assertThat(result.getStartAt()).isEqualTo(FIXED_TODAY.plusDays(1).atTime(15, 0));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        assertThat(result.getClientCompany()).isEqualTo("삼성전자");
        assertThat(result.getCategoryId()).isNull();
    }

    @Test
    void parseMeetingRoomAsMemoNotClientCompany() {
        String sourceText = "내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의";

        TaskParseResponseDTO result = parse(sourceText);

        assertThat(result.getSourceText()).isEqualTo(sourceText);
        assertThat(result.getStartAt()).isEqualTo(FIXED_TODAY.plusDays(1).atTime(15, 0));
        assertThat(result.getStartText()).isEqualTo("내일 오후 3시");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        assertThat(result.getTitle()).isEqualTo("팀 회의");
        assertThat(result.getClientCompany()).isNull();
        assertThat(result.getMemo()).isEqualTo("장소: SSAFY 1층 회의실");
        assertThat(result.getContent()).isNotEqualTo(sourceText);
    }

    @Test
    void parseKeepsCompanyNameAsClientCompany() {
        TaskParseResponseDTO result = parse("7월 1일 삼성전자와 계약 미팅");

        assertThat(result.getClientCompany()).isEqualTo("삼성전자");
        assertThat(result.getCategoryId()).isNull();
        assertThat(result.getStartAt()).isNull();
        assertThat(result.getStartDate().getMonthValue()).isEqualTo(7);
        assertThat(result.getStartDate().getDayOfMonth()).isEqualTo(1);
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_ONLY);
        assertThat(result.getMemo()).isNull();
    }

    @Test
    void parsePaidOnlySentenceDoesNotForceScheduleDates() {
        TaskParseResponseDTO result = parse("다음 주 금요일까지 300만원 입금 예정");

        assertThat(result.getTitle()).isEqualTo("입금 예정");
        assertThat(result.getBudget()).isEqualTo(3_000_000);
        assertThat(result.getPaidAt()).isNull();
        assertThat(result.getPaidDate()).isNotNull();
        assertThat(result.getPaidPrecision()).isEqualTo(DateTimePrecision.DATE_ONLY);
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.NONE);
        assertThat(result.getStartAt()).isNull();
        assertThat(result.getEndAt()).isNull();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("지급일"));
    }

    @Test
    void parseNormalizesAiCategoryAndPlaceLikeClientCompany() {
        LocalDateTime aiStartAt = LocalDateTime.of(2026, 6, 11, 15, 0);
        TaskParseService service = new TaskParseService(sourceText -> TaskParseResponseDTO.builder()
                .sourceText(sourceText)
                .categoryId(10)
                .title("팀 회의")
                .content("팀 회의")
                .memo(null)
                .startAt(aiStartAt)
                .endAt(null)
                .status(TaskStatus.IN_PROGRESS)
                .clientCompany("SSAFY 1층 회의실")
                .budget(null)
                .paidAt(null)
                .confidence(0.9)
                .warnings(List.of())
                .build(), FIXED_CLOCK);

        TaskParseResponseDTO result = parse(service, "내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의");

        assertThat(result.getCategoryId()).isNull();
        assertThat(result.getClientCompany()).isNull();
        assertThat(result.getMemo()).isEqualTo("장소: SSAFY 1층 회의실");
        assertThat(result.getContent()).isEqualTo("팀 회의");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseKnownCompanyWithoutSuffixAsClientCompany() {
        TaskParseResponseDTO result = parse("내일 10시 토스 로고 시안 회의");

        assertThat(result.getTitle()).isEqualTo("토스 로고 시안 회의");
        assertThat(result.getClientCompany()).isEqualTo("토스");
        assertThat(result.getMemo()).isNull();
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseKnownCompanyContainingRelativeDateWordWithoutDroppingTitle() {
        TaskParseResponseDTO result = parse("다음 주 화요일 15시 오늘의집 UX 인터뷰");

        assertThat(result.getTitle()).isEqualTo("오늘의집 UX 인터뷰");
        assertThat(result.getClientCompany()).isEqualTo("오늘의집");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseCompanyLikePlaceAsMemoNotClientCompany() {
        TaskParseResponseDTO result = parse("내일 오후 2시 네이버 1784 회의실에서 킥오프");

        assertThat(result.getTitle()).isEqualTo("킥오프");
        assertThat(result.getClientCompany()).isNull();
        assertThat(result.getMemo()).isEqualTo("장소: 네이버 1784 회의실");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseCompanyOfficePlaceAsMemoNotClientCompany() {
        TaskParseResponseDTO result = parse("6월 12일 10시 카카오 판교오피스에서 면담");

        assertThat(result.getTitle()).isEqualTo("면담");
        assertThat(result.getClientCompany()).isNull();
        assertThat(result.getMemo()).isEqualTo("장소: 카카오 판교오피스");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseSlashDateCompanyPlaceAsCleanMemo() {
        TaskParseResponseDTO result = parse("6/15 14시 토스 본사 3층에서 자료 리뷰");

        assertThat(result.getTitle()).isEqualTo("자료 리뷰");
        assertThat(result.getClientCompany()).isNull();
        assertThat(result.getMemo()).isEqualTo("장소: 토스 본사 3층");
        assertThat(result.getStartAt().toLocalDate().getMonthValue()).isEqualTo(6);
        assertThat(result.getStartAt().toLocalDate().getDayOfMonth()).isEqualTo(15);
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseKoreanFullDateDoesNotBecomeClientCompany() {
        TaskParseResponseDTO result = parse("2026년 7월 3일 프로젝트 종료 보고");

        assertThat(result.getTitle()).isEqualTo("프로젝트 종료 보고");
        assertThat(result.getClientCompany()).isNull();
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_ONLY);
    }

    @Test
    void parseZoomLinkAsOnlineAndLinkMemo() {
        TaskParseResponseDTO result = parse("내일 오후 2시 Zoom으로 킥오프, 링크는 https://zoom.us/j/123");

        assertThat(result.getTitle()).isEqualTo("킥오프");
        assertThat(result.getMemo()).isEqualTo("온라인: Zoom, 링크: https://zoom.us/j/123");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseMeetingRoomAndPreparationAsMemo() {
        TaskParseResponseDTO result = parse("6월 12일 10시 회의실 B에서 발표, 준비물 노트북");

        assertThat(result.getTitle()).isEqualTo("발표");
        assertThat(result.getMemo()).isEqualTo("장소: 회의실 B, 준비물: 노트북");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseBareMeetLinkAsLinkMemo() {
        TaskParseResponseDTO result = parse("오늘 15시 구글밋 디자인 리뷰, 링크는 meet.google.com/abc-defg-hij");

        assertThat(result.getTitle()).isEqualTo("디자인 리뷰");
        assertThat(result.getMemo()).isEqualTo("온라인: 구글밋, 링크: meet.google.com/abc-defg-hij");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseDocumentPreparationAsMaterialMemoWithoutDroppingTitleLinkWord() {
        TaskParseResponseDTO result = parse("6월 18일 14시 Figma 링크 확인, 자료는 PDF로 준비");

        assertThat(result.getTitle()).isEqualTo("Figma 링크 확인");
        assertThat(result.getMemo()).isEqualTo("자료: PDF로 준비");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseSlackHuddleKeepsTitleAndStoresExtraMemo() {
        TaskParseResponseDTO result = parse("오후 4시 Slack 허들, 사전 공유 문서 확인");

        assertThat(result.getTitle()).isEqualTo("Slack 허들");
        assertThat(result.getMemo())
                .contains("온라인: Slack")
                .contains("사전 공유 문서 확인")
                .contains("시간만 명시됨: 오후 4시");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.TIME_ONLY);
    }

    @Test
    void parseNotionBareDomainAsLinkMemo() {
        TaskParseResponseDTO result = parse("7월 1일 13시 Notion 회의록 정리, 링크는 notion.so/lancit");

        assertThat(result.getTitle()).isEqualTo("Notion 회의록 정리");
        assertThat(result.getMemo()).isEqualTo("링크: notion.so/lancit");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
    }

    @Test
    void parseDiscordMeetingKeepsTitleAndStoresPreparationMemo() {
        TaskParseResponseDTO result = parse("내일 18시 디스코드 음성 회의, 화면공유 준비");

        assertThat(result.getTitle()).isEqualTo("디스코드 음성 회의");
        assertThat(result.getMemo()).isEqualTo("온라인: 디스코드, 준비물: 화면공유");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
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
    void parseDateOnlyKeepsDateWithoutDefaultingStartAt() {
        TaskParseResponseDTO result = parse("7월 3일 네이버 프로젝트 킥오프 미팅");

        assertThat(result.getTitle()).isEqualTo("네이버 프로젝트 킥오프 미팅");
        assertThat(result.getStartAt()).isNull();
        assertThat(result.getStartDate().getMonthValue()).isEqualTo(7);
        assertThat(result.getStartDate().getDayOfMonth()).isEqualTo(3);
        assertThat(result.getStartTime()).isNull();
        assertThat(result.getStartText()).isEqualTo("7월 3일");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_ONLY);
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("날짜만 보존"));
    }

    @Test
    void parseTimeOnlyKeepsTimeWithoutDefaultingDate() {
        TaskParseResponseDTO result = parse("오후 3시에 포트폴리오 수정하기");

        assertThat(result.getStartAt()).isNull();
        assertThat(result.getStartDate()).isNull();
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(result.getStartText()).isEqualTo("오후 3시");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.TIME_ONLY);
        assertThat(result.getMemo()).isEqualTo("시간만 명시됨: 오후 3시");
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("시간만 보존"));
    }

    @Test
    void parseDurationCalculatesEndAtOnlyWhenStartAtIsExact() {
        TaskParseResponseDTO result = parse("내일 오후 3시부터 2시간 동안 포트폴리오 수정");

        assertThat(result.getTitle()).isEqualTo("포트폴리오 수정");
        assertThat(result.getStartAt()).isEqualTo(FIXED_TODAY.plusDays(1).atTime(15, 0));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        assertThat(result.getEndAt()).isEqualTo(FIXED_TODAY.plusDays(1).atTime(17, 0));
        assertThat(result.getEndText()).isEqualTo("2시간 동안");
        assertThat(result.getEndPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
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
        assertThat(result.getPaidAmount()).isEqualTo(3_000_000);
        assertThat(result.getPaidAt()).isNull();
        assertThat(result.getPaidDate()).isNotNull();
        assertThat(result.getPaidDate().getMonthValue()).isEqualTo(7);
        assertThat(result.getPaidDate().getDayOfMonth()).isEqualTo(1);
        assertThat(result.getPaidPrecision()).isEqualTo(DateTimePrecision.DATE_ONLY);
        assertThat(result.getStartAt()).isNull();
        assertThat(result.getWarnings()).anyMatch(warning -> warning.contains("지급일"));
    }

    @Test
    void parseScheduleAndPaidDateTimeSeparately() {
        TaskParseResponseDTO result = parse("내일 오전 10시 무신사 화보 회의, 잔금 50만원은 6월 20일 오후 3시 지급");

        assertThat(result.getTitle()).isEqualTo("무신사 화보 회의");
        assertThat(result.getClientCompany()).isEqualTo("무신사");
        assertThat(result.getStartAt()).isEqualTo(FIXED_TODAY.plusDays(1).atTime(10, 0));
        assertThat(result.getPaidAt()).isEqualTo(LocalDate.of(2027, 6, 20).atTime(15, 0));
        assertThat(result.getPaidPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        assertThat(result.getPaidAmount()).isEqualTo(500_000);
        assertThat(result.getBalanceAmount()).isEqualTo(500_000);
    }

    @Test
    void parsePaymentCenteredSentenceDoesNotSetScheduleStart() {
        TaskParseResponseDTO result = parse("오늘 오후 5시에 잔금 80만원 받기");

        assertThat(result.getTitle()).isEqualTo("잔금 받기");
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.NONE);
        assertThat(result.getStartAt()).isNull();
        assertThat(result.getPaidAt()).isEqualTo(FIXED_TODAY.atTime(17, 0));
        assertThat(result.getPaidPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        assertThat(result.getPaidAmount()).isEqualTo(800_000);
        assertThat(result.getBalanceAmount()).isEqualTo(800_000);
    }

    @Test
    void parseBudgetDepositAndBudgetRangeSeparately() {
        TaskParseResponseDTO mixedAmountResult = parse("오늘 14시 견적 회의, 견적 250만원, 계약금 50만원");

        assertThat(mixedAmountResult.getBudgetAmount()).isEqualTo(2_500_000);
        assertThat(mixedAmountResult.getDepositAmount()).isEqualTo(500_000);
        assertThat(mixedAmountResult.getPaidPrecision()).isEqualTo(DateTimePrecision.NONE);

        TaskParseResponseDTO budgetRangeResult = parse("내일 11시 견적서 발송, 예산은 100~150만원 범위");

        assertThat(budgetRangeResult.getBudget()).isNull();
        assertThat(budgetRangeResult.getBudgetText()).isEqualTo("100~150만원");
        assertThat(budgetRangeResult.getMemo()).contains("예산 범위: 100~150만원");
    }

    @Test
    void parseMultipleScheduleDatesKeepsFirstAndStoresRestInMemo() {
        TaskParseResponseDTO result = parse(
                "2026년 6월 15일 초안 제출, 2026년 6월 18일 피드백 반영, 2026년 6월 20일 최종 제출");

        assertThat(result.getTitle()).isEqualTo("초안 제출");
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_ONLY);
        assertThat(result.getMemo())
                .contains("피드백 반영: 2026-06-18")
                .contains("최종 제출: 2026-06-20");
    }

    @Test
    void parseSecondaryDeadlineDateTimeAsMemo() {
        TaskParseResponseDTO result = parse(
                "2026년 6월 10일 10시 회의하고 2026년 6월 12일 18시까지 수정본 전달");

        assertThat(result.getTitle()).isEqualTo("회의");
        assertThat(result.getStartAt()).isEqualTo(LocalDate.of(2026, 6, 10).atTime(10, 0));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_TIME);
        assertThat(result.getMemo()).contains("수정본 전달 마감: 2026-06-12T18:00:00");
    }

    @Test
    void parseScheduleAndPaymentDateInMultiDateSentence() {
        TaskParseResponseDTO result = parse("6월 30일 프로젝트 종료, 7월 5일 잔금 입금");

        assertThat(result.getTitle()).isEqualTo("프로젝트 종료");
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(result.getStartPrecision()).isEqualTo(DateTimePrecision.DATE_ONLY);
        assertThat(result.getPaidDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(result.getPaidPrecision()).isEqualTo(DateTimePrecision.DATE_ONLY);
        assertThat(result.getMemo()).isNull();
    }

    private TaskParseResponseDTO parse(String sourceText) {
        return parse(taskParseService, sourceText);
    }

    private TaskParseResponseDTO parse(TaskParseService service, String sourceText) {
        return service.parse(TaskParseRequestDTO.builder().sourceText(sourceText).build());
    }
}
