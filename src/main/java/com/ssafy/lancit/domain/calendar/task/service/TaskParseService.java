package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.global.enums.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TaskParseService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);

    private static final Pattern BUDGET_PATTERN =
            Pattern.compile("(?:예산|금액|비용)?\\s*[:：]?\\s*([0-9][0-9,]*)\\s*(만\\s*원|원)");
    private static final Pattern ISO_DATE_PATTERN =
            Pattern.compile("\\b(\\d{4})[-.](\\d{1,2})[-.](\\d{1,2})\\b");
    private static final Pattern MONTH_DAY_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
    private static final Pattern RELATIVE_DATE_PATTERN =
            Pattern.compile("오늘|내일|모레");
    private static final Pattern WEEKDAY_PATTERN =
            Pattern.compile("(이번|다음)\\s*주\\s*(월|화|수|목|금|토|일)요일");
    private static final Pattern CLOCK_RANGE_PATTERN =
            Pattern.compile("(?:(오전|오후)\\s*)?(\\d{1,2})(?::(\\d{2})|\\s*시(?:\\s*(\\d{1,2})\\s*분)?)\\s*(?:~|-)\\s*(?:(오전|오후)\\s*)?(\\d{1,2})(?::(\\d{2})|\\s*시(?:\\s*(\\d{1,2})\\s*분)?)");
    private static final Pattern KOREAN_FROM_TO_PATTERN =
            Pattern.compile("(?:(오전|오후)\\s*)?(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분)?\\s*부터\\s*(?:(오전|오후)\\s*)?(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분)?\\s*까지");
    private static final Pattern SINGLE_TIME_PATTERN =
            Pattern.compile("(?:(오전|오후)\\s*)?(\\d{1,2})(?::(\\d{2})|\\s*시(?:\\s*(\\d{1,2})\\s*분)?)(?:\\s*(?:에|에는|부터|까지))?");
    private static final Pattern PAID_KEYWORD_PATTERN =
            Pattern.compile("입금|지급|정산");
    private static final Pattern CATEGORY_KEYWORD_PATTERN =
            Pattern.compile("회의|미팅|계약|정산|입금|지급");
    private static final Pattern COMPANY_SUFFIX_PATTERN =
            Pattern.compile("([가-힣A-Za-z0-9]+(?:회사|전자|테크|소프트|그룹|은행|보험|카드|커머스|랩스|시스템즈|산업|주식회사))");
    private static final Pattern COMPANY_REQUEST_PATTERN =
            Pattern.compile("([가-힣A-Za-z0-9]+)\\s*(?:의뢰|발주)|(?:고객사|클라이언트|발주처)\\s*([가-힣A-Za-z0-9]+)");
    private static final Pattern COMPANY_WITH_PARTICLE_PATTERN =
            Pattern.compile("([가-힣A-Za-z0-9]+)\\s*(?:랑|와|과)\\s*(?:미팅|회의|계약|상담)");
    private static final Pattern PROJECT_COMPANY_PATTERN =
            Pattern.compile("([가-힣A-Za-z0-9]+)\\s+프로젝트\\s*(?:회의|미팅|작업)?");
    private static final Pattern PLACE_BEFORE_PARTICLE_PATTERN =
            Pattern.compile("([^\\n,，;；/]{1,80}?)\\s*(?:에서|에서의)(?=\\s|$)");
    private static final Pattern URL_PATTERN =
            Pattern.compile("(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern ONLINE_MEMO_PATTERN =
            Pattern.compile("(?i)(온라인|zoom|줌|google\\s*meet|구글\\s*밋|teams|팀즈)");
    private static final Set<String> COMPANY_STOP_WORDS = Set.of(
            "오늘", "내일", "모레", "이번", "다음", "프로젝트", "계약", "정산",
            "입금", "지급", "예산", "금액", "비용", "회의", "미팅", "작업",
            "외주", "예정", "팀", "장소", "주소", "온라인", "회의실", "카페"
    );

    private final AiTaskParseClient aiTaskParseClient;

    public TaskParseService() {
        this(null);
    }

    @Autowired(required = false)
    public TaskParseService(AiTaskParseClient aiTaskParseClient) {
        this.aiTaskParseClient = aiTaskParseClient;
    }

    public TaskParseResponseDTO parse(TaskParseRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getSourceText() == null || requestDTO.getSourceText().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        String sourceText = requestDTO.getSourceText().trim();
        TaskParseResponseDTO aiResult = tryParseWithAi(sourceText);
        if (aiResult != null) {
            return aiResult;
        }

        return parseByRules(sourceText);
    }

    private TaskParseResponseDTO tryParseWithAi(String sourceText) {
        if (aiTaskParseClient == null) {
            return null;
        }

        try {
            TaskParseResponseDTO aiResult = aiTaskParseClient.parse(sourceText);
            validateAiResult(aiResult);
            normalizeAiResult(aiResult, sourceText);
            return aiResult;
        } catch (RuntimeException e) {
            log.warn("AI task parsing failed. Falling back to rule-based parser. reason={}", e.getClass().getSimpleName());
            return null;
        }
    }

    private void validateAiResult(TaskParseResponseDTO aiResult) {
        if (aiResult == null) {
            throw new IllegalStateException("AI task parse result is empty");
        }
        if (!StringUtils.hasText(aiResult.getTitle())) {
            throw new IllegalStateException("AI task parse result has no title");
        }
        if (aiResult.getStatus() == null) {
            throw new IllegalStateException("AI task parse result has no status");
        }
    }

    private void normalizeAiResult(TaskParseResponseDTO aiResult, String sourceText) {
        aiResult.setSourceText(sourceText);
        aiResult.setContent(normalizeContent(aiResult.getContent(), sourceText));

        MemoResult ruleMemo = extractMemo(sourceText);
        if (!StringUtils.hasText(aiResult.getMemo())) {
            aiResult.setMemo(ruleMemo.memo());
        } else {
            aiResult.setMemo(normalizeOptionalText(aiResult.getMemo()));
        }

        String clientCompany = normalizeOptionalText(aiResult.getClientCompany());
        if (clientCompany != null && !isValidClientCompanyCandidate(clientCompany)) {
            aiResult.setMemo(appendMemo(aiResult.getMemo(), clientCompany));
            clientCompany = null;
        }
        aiResult.setClientCompany(clientCompany);

        if (aiResult.getWarnings() == null) {
            aiResult.setWarnings(List.of());
        }
        if (aiResult.getConfidence() != null) {
            aiResult.setConfidence(Math.max(0.0, Math.min(1.0, aiResult.getConfidence())));
        }
    }

    private TaskParseResponseDTO parseByRules(String sourceText) {
        List<String> warnings = new ArrayList<>();

        Integer budget = extractBudget(sourceText, warnings);
        MemoResult memoResult = extractMemo(sourceText);
        TitleResult titleResult = extractTitle(sourceText, memoResult.parts());
        String sourceWithoutMemo = removeMemoParts(sourceText, memoResult.parts());
        String clientCompany = extractClientCompany(titleResult.title(), sourceWithoutMemo);

        LocalDate today = LocalDate.now(SEOUL_ZONE);
        List<DateCandidate> dateCandidates = extractDateCandidates(sourceText, today, warnings);
        DateCandidate paidDateCandidate = findPaidDateCandidate(sourceText, dateCandidates);
        DateCandidate scheduleDateCandidate = chooseScheduleDateCandidate(dateCandidates, paidDateCandidate);
        ParsedTime parsedTime = extractTime(sourceText, warnings);

        LocalDateTime paidAt = null;
        if (paidDateCandidate != null) {
            paidAt = paidDateCandidate.date().atStartOfDay();
            warnings.add("지급일로 보이는 날짜를 감지했습니다. 실제 지급일은 확인해 주세요.");
        }

        LocalDate scheduleDate = null;
        if (scheduleDateCandidate != null) {
            scheduleDate = scheduleDateCandidate.date();
        } else if (parsedTime.start() != null) {
            scheduleDate = today;
            warnings.add("날짜 없이 시간만 입력되어 오늘 날짜로 임시 설정했습니다.");
        }

        LocalDateTime startAt = null;
        LocalDateTime endAt = null;
        boolean defaultedStartTime = false;
        if (scheduleDate != null) {
            LocalTime startTime = parsedTime.start();
            if (startTime == null) {
                startTime = DEFAULT_START_TIME;
                defaultedStartTime = true;
                warnings.add("시간 없이 날짜만 입력되어 오전 9시로 임시 설정했습니다.");
            }
            startAt = LocalDateTime.of(scheduleDate, startTime);

            if (parsedTime.end() != null) {
                endAt = LocalDateTime.of(scheduleDate, parsedTime.end());
                if (endAt.isBefore(startAt)) {
                    warnings.add("종료 시간이 시작 시간보다 앞서 종료 시간을 확정하지 않았습니다.");
                    endAt = null;
                }
            } else if (parsedTime.start() != null) {
                warnings.add("종료 시간을 인식하지 못했습니다.");
            }
        } else {
            warnings.add("일정 시작일을 확실히 인식하지 못했습니다.");
        }

        if (paidDateCandidate != null && scheduleDateCandidate == null) {
            warnings.add("지급일로 보이는 날짜는 일정 시작일로 자동 확정하지 않았습니다.");
        }
        if (parsedTime.ambiguousMeridiem()) {
            warnings.add("오전/오후가 없어 시간 해석이 모호합니다.");
        }
        if (!titleResult.reliable()) {
            warnings.add("제목을 확실히 추출하지 못해 원문 일부를 사용했습니다.");
        }
        if (clientCompany == null) {
            warnings.add("회사명을 확실히 인식하지 못했습니다.");
        }
        addCategoryWarning(sourceText, warnings);

        double confidence = calculateConfidence(
                titleResult.reliable(),
                scheduleDateCandidate != null,
                parsedTime.start() != null,
                endAt != null,
                budget != null,
                paidAt != null,
                clientCompany != null,
                startAt != null,
                defaultedStartTime
        );

        return TaskParseResponseDTO.builder()
                .sourceText(sourceText)
                .categoryId(null)
                .title(titleResult.title())
                .content(null)
                .memo(memoResult.memo())
                .startAt(startAt)
                .endAt(endAt)
                .status(TaskStatus.IN_PROGRESS)
                .clientCompany(clientCompany)
                .budget(budget)
                .paidAt(paidAt)
                .confidence(confidence)
                .warnings(warnings)
                .build();
    }

    private MemoResult extractMemo(String sourceText) {
        List<String> memoParts = new ArrayList<>();

        Matcher placeMatcher = PLACE_BEFORE_PARTICLE_PATTERN.matcher(sourceText);
        while (placeMatcher.find()) {
            addMemoPart(memoParts, cleanMemoCandidate(placeMatcher.group(1)));
        }

        Matcher urlMatcher = URL_PATTERN.matcher(sourceText);
        while (urlMatcher.find()) {
            addMemoPart(memoParts, normalizeOptionalText(urlMatcher.group()));
        }

        Matcher onlineMatcher = ONLINE_MEMO_PATTERN.matcher(sourceText);
        while (onlineMatcher.find()) {
            addMemoPart(memoParts, normalizeOptionalText(onlineMatcher.group()));
        }

        if (memoParts.isEmpty()) {
            return new MemoResult(null, List.of());
        }
        return new MemoResult(String.join(" / ", memoParts), List.copyOf(memoParts));
    }

    private void addMemoPart(List<String> memoParts, String memoPart) {
        if (!StringUtils.hasText(memoPart)) {
            return;
        }
        String normalizedMemoPart = memoPart.trim();
        if (!memoParts.contains(normalizedMemoPart)) {
            memoParts.add(normalizedMemoPart);
        }
    }

    private String cleanMemoCandidate(String candidate) {
        String memo = removeStructuredExpressions(candidate);
        memo = memo.replaceAll("^(?:장소|주소)\\s*[:：]?\\s*", "");
        memo = memo.replaceAll("\\s*(?:에|에는|부터|까지|에서|으로|로)$", "");
        memo = memo.replaceAll("\\s+", " ").trim();
        return normalizeOptionalText(memo);
    }

    private String removeStructuredExpressions(String text) {
        String result = BUDGET_PATTERN.matcher(text).replaceAll(" ");
        result = ISO_DATE_PATTERN.matcher(result).replaceAll(" ");
        result = MONTH_DAY_PATTERN.matcher(result).replaceAll(" ");
        result = WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = RELATIVE_DATE_PATTERN.matcher(result).replaceAll(" ");
        result = KOREAN_FROM_TO_PATTERN.matcher(result).replaceAll(" ");
        result = CLOCK_RANGE_PATTERN.matcher(result).replaceAll(" ");
        result = SINGLE_TIME_PATTERN.matcher(result).replaceAll(" ");
        return result.replaceAll("\\s+", " ").trim();
    }

    private String removeMemoParts(String text, List<String> memoParts) {
        String result = text;
        for (String memoPart : memoParts) {
            result = Pattern.compile(Pattern.quote(memoPart) + "\\s*(?:에서의|에서|으로|로)?")
                    .matcher(result)
                    .replaceAll(" ");
        }
        return result.replaceAll("\\s+", " ").trim();
    }

    private String normalizeContent(String content, String sourceText) {
        String normalizedContent = normalizeOptionalText(content);
        if (normalizedContent == null || normalizedContent.equals(sourceText)) {
            return null;
        }
        return normalizedContent;
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String appendMemo(String memo, String memoPart) {
        String normalizedMemo = normalizeOptionalText(memo);
        String normalizedMemoPart = normalizeOptionalText(memoPart);
        if (normalizedMemoPart == null) {
            return normalizedMemo;
        }
        if (normalizedMemo == null) {
            return normalizedMemoPart;
        }
        if (normalizedMemo.equals(normalizedMemoPart) || normalizedMemo.contains(normalizedMemoPart)) {
            return normalizedMemo;
        }
        return normalizedMemo + " / " + normalizedMemoPart;
    }

    private Integer extractBudget(String sourceText, List<String> warnings) {
        Matcher matcher = BUDGET_PATTERN.matcher(sourceText);
        if (!matcher.find()) {
            return null;
        }

        try {
            long amount = Long.parseLong(matcher.group(1).replace(",", ""));
            if ("만원".equals(matcher.group(2).replaceAll("\\s+", ""))) {
                amount = Math.multiplyExact(amount, 10000L);
            }
            if (amount > Integer.MAX_VALUE) {
                warnings.add("금액이 너무 커서 예산으로 확정하지 못했습니다.");
                return null;
            }
            return (int) amount;
        } catch (ArithmeticException | NumberFormatException e) {
            warnings.add("금액을 인식하지 못했습니다.");
            return null;
        }
    }

    private TitleResult extractTitle(String sourceText, List<String> memoParts) {
        String firstLine = sourceText.split("\\R", 2)[0].trim();
        String title = BUDGET_PATTERN.matcher(firstLine).replaceAll("");
        title = ISO_DATE_PATTERN.matcher(title).replaceAll(" ");
        title = MONTH_DAY_PATTERN.matcher(title).replaceAll(" ");
        title = WEEKDAY_PATTERN.matcher(title).replaceAll(" ");
        title = RELATIVE_DATE_PATTERN.matcher(title).replaceAll(" ");
        title = KOREAN_FROM_TO_PATTERN.matcher(title).replaceAll(" ");
        title = CLOCK_RANGE_PATTERN.matcher(title).replaceAll(" ");
        title = SINGLE_TIME_PATTERN.matcher(title).replaceAll(" ");
        title = removeMemoParts(title, memoParts);
        title = title.replaceAll("[,，;；/]+", " ");
        title = title.replaceAll("\\s+", " ").trim();
        title = title.replaceAll("^(?:에|에는|부터|까지)\\s*", "").trim();
        title = title.replaceAll("\\s*(?:에|에는|부터|까지|에서|으로|로)$", "").trim();

        if (title.isEmpty()) {
            String fallback = firstLine.length() > 40 ? firstLine.substring(0, 40).trim() : firstLine;
            return new TitleResult(fallback, false);
        }
        return new TitleResult(title, true);
    }

    private List<DateCandidate> extractDateCandidates(String sourceText, LocalDate today, List<String> warnings) {
        List<DateCandidate> candidates = new ArrayList<>();

        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(sourceText);
        while (isoMatcher.find()) {
            LocalDate date = createDate(
                    parseInt(isoMatcher.group(1)),
                    parseInt(isoMatcher.group(2)),
                    parseInt(isoMatcher.group(3)),
                    isoMatcher.group(),
                    warnings
            );
            if (date != null) {
                candidates.add(new DateCandidate(date, isoMatcher.start(), isoMatcher.end()));
            }
        }

        Matcher monthDayMatcher = MONTH_DAY_PATTERN.matcher(sourceText);
        while (monthDayMatcher.find()) {
            LocalDate date = resolveMonthDay(
                    parseInt(monthDayMatcher.group(1)),
                    parseInt(monthDayMatcher.group(2)),
                    today,
                    monthDayMatcher.group(),
                    warnings
            );
            if (date != null) {
                candidates.add(new DateCandidate(date, monthDayMatcher.start(), monthDayMatcher.end()));
            }
        }

        Matcher relativeMatcher = RELATIVE_DATE_PATTERN.matcher(sourceText);
        while (relativeMatcher.find()) {
            candidates.add(new DateCandidate(
                    resolveRelativeDate(relativeMatcher.group(), today),
                    relativeMatcher.start(),
                    relativeMatcher.end()
            ));
        }

        Matcher weekdayMatcher = WEEKDAY_PATTERN.matcher(sourceText);
        while (weekdayMatcher.find()) {
            LocalDate date = resolveWeekday(weekdayMatcher.group(1), weekdayMatcher.group(2), today);
            if (date.isBefore(today)) {
                warnings.add("이번 주 날짜가 이미 지난 날짜일 수 있습니다.");
            }
            candidates.add(new DateCandidate(date, weekdayMatcher.start(), weekdayMatcher.end()));
        }

        candidates.sort(Comparator.comparingInt(DateCandidate::start));
        return candidates;
    }

    private DateCandidate findPaidDateCandidate(String sourceText, List<DateCandidate> dateCandidates) {
        for (int i = 0; i < dateCandidates.size(); i++) {
            DateCandidate candidate = dateCandidates.get(i);
            int nextDateStart = i + 1 < dateCandidates.size() ? dateCandidates.get(i + 1).start() : sourceText.length();
            int afterTo = Math.min(sourceText.length(), Math.min(candidate.end() + 24, nextDateStart));
            int beforeFrom = Math.max(0, candidate.start() - 12);
            String nearDate = sourceText.substring(beforeFrom, candidate.start())
                    + sourceText.substring(candidate.end(), afterTo);

            if (PAID_KEYWORD_PATTERN.matcher(nearDate).find()) {
                return candidate;
            }
        }
        return null;
    }

    private DateCandidate chooseScheduleDateCandidate(List<DateCandidate> dateCandidates, DateCandidate paidDateCandidate) {
        for (DateCandidate candidate : dateCandidates) {
            if (!sameSpan(candidate, paidDateCandidate)) {
                return candidate;
            }
        }
        return null;
    }

    private ParsedTime extractTime(String sourceText, List<String> warnings) {
        Matcher fromToMatcher = KOREAN_FROM_TO_PATTERN.matcher(sourceText);
        if (fromToMatcher.find()) {
            TimeToken start = parseTimeToken(
                    fromToMatcher.group(1),
                    fromToMatcher.group(2),
                    null,
                    fromToMatcher.group(3),
                    null,
                    warnings
            );
            TimeToken end = parseTimeToken(
                    fromToMatcher.group(4),
                    fromToMatcher.group(5),
                    null,
                    fromToMatcher.group(6),
                    fromToMatcher.group(1),
                    warnings
            );
            return new ParsedTime(
                    start == null ? null : start.time(),
                    end == null ? null : end.time(),
                    start != null,
                    isAmbiguous(start) || isAmbiguous(end)
            );
        }

        Matcher clockRangeMatcher = CLOCK_RANGE_PATTERN.matcher(sourceText);
        if (clockRangeMatcher.find()) {
            TimeToken start = parseTimeToken(
                    clockRangeMatcher.group(1),
                    clockRangeMatcher.group(2),
                    clockRangeMatcher.group(3),
                    clockRangeMatcher.group(4),
                    null,
                    warnings
            );
            TimeToken end = parseTimeToken(
                    clockRangeMatcher.group(5),
                    clockRangeMatcher.group(6),
                    clockRangeMatcher.group(7),
                    clockRangeMatcher.group(8),
                    clockRangeMatcher.group(1),
                    warnings
            );
            return new ParsedTime(
                    start == null ? null : start.time(),
                    end == null ? null : end.time(),
                    start != null,
                    isAmbiguous(start) || isAmbiguous(end)
            );
        }

        Matcher singleTimeMatcher = SINGLE_TIME_PATTERN.matcher(sourceText);
        if (singleTimeMatcher.find()) {
            TimeToken start = parseTimeToken(
                    singleTimeMatcher.group(1),
                    singleTimeMatcher.group(2),
                    singleTimeMatcher.group(3),
                    singleTimeMatcher.group(4),
                    null,
                    warnings
            );
            return new ParsedTime(
                    start == null ? null : start.time(),
                    null,
                    start != null,
                    isAmbiguous(start)
            );
        }

        return new ParsedTime(null, null, false, false);
    }

    private TimeToken parseTimeToken(String meridiem,
                                     String hourText,
                                     String colonMinuteText,
                                     String koreanMinuteText,
                                     String inheritedMeridiem,
                                     List<String> warnings) {
        try {
            int originalHour = Integer.parseInt(hourText);
            int minute = parseMinute(colonMinuteText, koreanMinuteText);
            String effectiveMeridiem = meridiem == null ? inheritedMeridiem : meridiem;

            if (minute < 0 || minute > 59) {
                warnings.add("분 단위 시간이 올바르지 않아 시간을 확정하지 못했습니다.");
                return null;
            }
            if (effectiveMeridiem != null && originalHour > 12) {
                warnings.add("오전/오후 표현과 13시 이상 시간이 함께 입력되어 시간을 확정하지 못했습니다.");
                return null;
            }

            int hour = originalHour;
            if ("오전".equals(effectiveMeridiem) && hour == 12) {
                hour = 0;
            } else if ("오후".equals(effectiveMeridiem) && hour < 12) {
                hour += 12;
            }

            if (hour < 0 || hour > 23) {
                warnings.add("시간 표현을 인식하지 못했습니다.");
                return null;
            }

            boolean ambiguousMeridiem = effectiveMeridiem == null && originalHour >= 1 && originalHour <= 11;
            return new TimeToken(LocalTime.of(hour, minute), ambiguousMeridiem);
        } catch (NumberFormatException | DateTimeException e) {
            warnings.add("시간 표현을 인식하지 못했습니다.");
            return null;
        }
    }

    private int parseMinute(String colonMinuteText, String koreanMinuteText) {
        if (colonMinuteText != null) {
            return Integer.parseInt(colonMinuteText);
        }
        if (koreanMinuteText != null) {
            return Integer.parseInt(koreanMinuteText);
        }
        return 0;
    }

    private LocalDate createDate(int year, int month, int day, String expression, List<String> warnings) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            warnings.add("날짜 표현을 인식하지 못했습니다: " + expression);
            return null;
        }
    }

    private LocalDate resolveMonthDay(int month, int day, LocalDate today, String expression, List<String> warnings) {
        try {
            LocalDate date = LocalDate.of(today.getYear(), month, day);
            if (date.isBefore(today)) {
                date = date.plusYears(1);
                warnings.add("연도가 없는 날짜가 이미 지나 다음 해로 추정했습니다.");
            }
            return date;
        } catch (DateTimeException e) {
            warnings.add("날짜 표현을 인식하지 못했습니다: " + expression);
            return null;
        }
    }

    private LocalDate resolveRelativeDate(String expression, LocalDate today) {
        return switch (expression) {
            case "내일" -> today.plusDays(1);
            case "모레" -> today.plusDays(2);
            default -> today;
        };
    }

    private LocalDate resolveWeekday(String weekText, String dayText, LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int weekOffset = "다음".equals(weekText) ? 7 : 0;
        return monday.plusDays(weekOffset + toDayOfWeek(dayText).getValue() - 1L);
    }

    private DayOfWeek toDayOfWeek(String dayText) {
        return switch (dayText) {
            case "월" -> DayOfWeek.MONDAY;
            case "화" -> DayOfWeek.TUESDAY;
            case "수" -> DayOfWeek.WEDNESDAY;
            case "목" -> DayOfWeek.THURSDAY;
            case "금" -> DayOfWeek.FRIDAY;
            case "토" -> DayOfWeek.SATURDAY;
            default -> DayOfWeek.SUNDAY;
        };
    }

    private int parseInt(String value) {
        return Integer.parseInt(value);
    }

    private String extractClientCompany(String title, String sourceText) {
        String normalizedTitle = title.replaceAll("\\s+", " ").trim();
        String normalizedSourceText = sourceText.replaceAll("\\s+", " ").trim();
        String company = findCompany(COMPANY_REQUEST_PATTERN, normalizedSourceText, 1, 2);
        if (company != null) {
            return company;
        }
        company = findCompany(COMPANY_SUFFIX_PATTERN, normalizedTitle, 1);
        if (company != null) {
            return company;
        }
        company = findCompany(COMPANY_WITH_PARTICLE_PATTERN, normalizedSourceText, 1);
        if (company != null) {
            return company;
        }
        return findCompany(PROJECT_COMPANY_PATTERN, normalizedTitle, 1);
    }

    private String findCompany(Pattern pattern, String text, int... groupNumbers) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            for (int groupNumber : groupNumbers) {
                String candidate = matcher.group(groupNumber);
                if (isValidClientCompanyCandidate(candidate)) {
                    return candidate.trim();
                }
            }
        }
        return null;
    }

    private boolean isValidClientCompanyCandidate(String candidate) {
        String normalizedCandidate = normalizeOptionalText(candidate);
        if (normalizedCandidate == null
                || normalizedCandidate.length() < 2
                || COMPANY_STOP_WORDS.contains(normalizedCandidate)) {
            return false;
        }

        String lowerCandidate = normalizedCandidate.toLowerCase();
        return !normalizedCandidate.matches("\\d+\\s*층")
                && !normalizedCandidate.contains("회의실")
                && !normalizedCandidate.contains("카페")
                && !normalizedCandidate.contains("주소")
                && !normalizedCandidate.matches(".*\\S역(?:\\s|$).*")
                && !lowerCandidate.contains("online")
                && !lowerCandidate.contains("zoom")
                && !lowerCandidate.contains("meet")
                && !lowerCandidate.contains("teams")
                && !normalizedCandidate.contains("온라인")
                && !normalizedCandidate.contains("줌")
                && !normalizedCandidate.contains("구글 밋")
                && !normalizedCandidate.contains("팀즈");
    }

    private void addCategoryWarning(String sourceText, List<String> warnings) {
        Matcher matcher = CATEGORY_KEYWORD_PATTERN.matcher(sourceText);
        if (matcher.find()) {
            warnings.add("카테고리 키워드(" + matcher.group() + ")를 감지했지만 categoryId는 자동 지정하지 않았습니다.");
            return;
        }
        warnings.add("카테고리는 사용자별 데이터 확인이 필요해 자동 지정하지 않았습니다.");
    }

    private double calculateConfidence(boolean titleReliable,
                                       boolean dateFound,
                                       boolean timeFound,
                                       boolean endFound,
                                       boolean budgetFound,
                                       boolean paidAtFound,
                                       boolean clientCompanyFound,
                                       boolean startAtFound,
                                       boolean defaultedStartTime) {
        double confidence = 0.35;
        if (titleReliable) {
            confidence += 0.2;
        }
        if (dateFound) {
            confidence += 0.15;
        }
        if (timeFound) {
            confidence += 0.1;
        }
        if (endFound) {
            confidence += 0.05;
        }
        if (budgetFound) {
            confidence += 0.05;
        }
        if (paidAtFound) {
            confidence += 0.03;
        }
        if (clientCompanyFound) {
            confidence += 0.05;
        }
        if (startAtFound && titleReliable) {
            confidence = Math.max(confidence, 0.75);
        } else if (titleReliable) {
            confidence = Math.max(confidence, 0.55);
        }
        if (defaultedStartTime) {
            confidence -= 0.05;
        }
        return Math.max(0.1, Math.min(0.95, Math.round(confidence * 100.0) / 100.0));
    }

    private boolean sameSpan(DateCandidate candidate, DateCandidate other) {
        return candidate != null
                && other != null
                && candidate.start() == other.start()
                && candidate.end() == other.end();
    }

    private boolean isAmbiguous(TimeToken token) {
        return token != null && token.ambiguousMeridiem();
    }

    private record TitleResult(String title, boolean reliable) {
    }

    private record MemoResult(String memo, List<String> parts) {
    }

    private record DateCandidate(LocalDate date, int start, int end) {
    }

    private record ParsedTime(LocalTime start, LocalTime end, boolean found, boolean ambiguousMeridiem) {
    }

    private record TimeToken(LocalTime time, boolean ambiguousMeridiem) {
    }
}
