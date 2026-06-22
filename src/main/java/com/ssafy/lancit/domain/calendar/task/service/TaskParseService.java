package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.global.enums.DateTimePrecision;
import com.ssafy.lancit.global.enums.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
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

    private static final Pattern BUDGET_PATTERN =
            Pattern.compile("(?:예산|금액|비용)?\\s*[:：]?\\s*([0-9][0-9,]*)\\s*(만\\s*원|원)");
    private static final Pattern AMOUNT_PATTERN =
            Pattern.compile("([0-9][0-9,]*)\\s*(만\\s*원|원)");
    private static final Pattern AMOUNT_RANGE_PATTERN =
            Pattern.compile("([0-9][0-9,]*)\\s*~\\s*([0-9][0-9,]*)\\s*(만\\s*원|원)");
    private static final Pattern PERCENT_PAYMENT_PATTERN =
            Pattern.compile("(?:선금|계약금|잔금)[^,，;；]*\\d{1,3}\\s*%");
    private static final Pattern ISO_DATE_PATTERN =
            Pattern.compile("\\b(\\d{4})[-.](\\d{1,2})[-.](\\d{1,2})\\b");
    private static final Pattern KOREAN_FULL_DATE_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
    private static final Pattern MONTH_DAY_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일");
    private static final Pattern SLASH_DATE_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{1,2})/(\\d{1,2})(?!\\d)");
    private static final Pattern NEXT_MONTH_DAY_PATTERN =
            Pattern.compile("다음\\s*달\\s*(\\d{1,2})\\s*일");
    private static final Pattern THIS_MONTH_END_PATTERN =
            Pattern.compile("이번\\s*달\\s*말");
    private static final Pattern MONTH_END_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{1,2})\\s*월\\s*말");
    private static final Pattern RELATIVE_DATE_PATTERN =
            Pattern.compile("(?<![가-힣A-Za-z0-9])(오늘|내일|모레)(?![가-힣A-Za-z0-9])");
    private static final Pattern WEEKDAY_PATTERN =
            Pattern.compile("(이번|다음)\\s*주\\s*(월|화|수|목|금|토|일)요일");
    private static final Pattern DIRECT_WEEKDAY_PATTERN =
            Pattern.compile("(이번|다음)\\s*(월|화|수|목|금|토|일)요일");
    private static final Pattern BARE_WEEKDAY_PATTERN =
            Pattern.compile("(?<![가-힣A-Za-z0-9])(월|화|수|목|금|토|일)요일");
    private static final Pattern SAME_WEEKDAY_PATTERN =
            Pattern.compile("같은\\s*주\\s*(월|화|수|목|금|토|일)요일");
    private static final Pattern MONTH_ORDINAL_WEEKDAY_PATTERN =
            Pattern.compile("(?<!\\d)(\\d{1,2})\\s*월\\s*(첫째|둘째|셋째|넷째|다섯째)\\s*주\\s*(월|화|수|목|금|토|일)요일");
    private static final Pattern NEXT_MONTH_ORDINAL_WEEKDAY_PATTERN =
            Pattern.compile("다음\\s*달\\s*(첫째|둘째|셋째|넷째|다섯째)\\s*(월|화|수|목|금|토|일)요일");
    private static final Pattern CLOCK_RANGE_PATTERN =
            Pattern.compile("(?:(오전|오후)\\s*)?(\\d{1,2})(?::(\\d{2})|\\s*시(?!간)(?:\\s*(\\d{1,2})\\s*분)?)\\s*(?:~|-)\\s*(?:(오전|오후)\\s*)?(\\d{1,2})(?::(\\d{2})|\\s*시(?!간)(?:\\s*(\\d{1,2})\\s*분)?)");
    private static final Pattern KOREAN_FROM_TO_PATTERN =
            Pattern.compile("(?:(오전|오후)\\s*)?(\\d{1,2})\\s*시(?!간)(?:\\s*(\\d{1,2})\\s*분)?\\s*부터\\s*(?:(오전|오후)\\s*)?(\\d{1,2})\\s*시(?!간)(?:\\s*(\\d{1,2})\\s*분)?\\s*까지");
    private static final Pattern SINGLE_TIME_PATTERN =
            Pattern.compile("(?:(오전|오후)\\s*)?(\\d{1,2})(?::(\\d{2})|\\s*시(?!간)(?:\\s*(\\d{1,2})\\s*분)?)(?:\\s*(?:에|에는|부터|까지))?");
    private static final Pattern DURATION_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*시간(?:\\s*(\\d{1,2})\\s*분)?\\s*(?:동안)?|(\\d{1,3})\\s*분\\s*(?:동안)?");
    private static final Pattern PAID_KEYWORD_PATTERN =
            Pattern.compile("입금일|지급일|입금(?!액)|지급|정산|송금|받기|지급받기");
    private static final Pattern PAYMENT_CONTEXT_PATTERN =
            Pattern.compile("입금|지급|정산|송금|받기|지급받기|입금일|지급일|계약금|선금|잔금|입금액");
    private static final Pattern PAID_NEXT_DAY_PATTERN =
            Pattern.compile("(?:다음\\s*날[^,，;；]*(?:입금|지급|정산)|(?:입금|지급|정산)[^,，;；]*다음\\s*날)");
    private static final Pattern PAID_SAME_DAY_PATTERN =
            Pattern.compile("(?:당일|같은\\s*날)[^,，;；]*(?:입금|지급|정산)|(?:입금|지급|정산)[^,，;；]*(?:당일|같은\\s*날)");
    private static final Pattern FLEXIBLE_TIME_PATTERN =
            Pattern.compile("(정오)|(?:(오전|오후|저녁|밤|새벽|아침|퇴근\\s*후)\\s*)?(\\d{1,2})(?::(\\d{2})|\\s*시(?!간)(?:\\s*(반|\\d{1,2}\\s*분))?)");
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
            Pattern.compile("([^\\n,，;；]{1,80}?)\\s*(?:에서|에서의)(?=\\s|$)");
    private static final Pattern PLACE_CONTEXT_PATTERN =
            Pattern.compile("(?i)(회의실|미팅룸|카페|본사|사옥|오피스|office|타워|로비|캠퍼스|스튜디오|라이브러리|홍대점|지점|매장|층|앞|파크|1784)");
    private static final Pattern URL_PATTERN =
            Pattern.compile("(?i)\\b(?:(?:https?://|www\\.)[^\\s,，;；]+|(?:[a-z0-9-]+\\.)+(?:com|net|org|co|io|so|us|kr|dev|app|ai|me)(?:/[^\\s,，;；]*)?)");
    private static final Pattern ONLINE_MEMO_PATTERN =
            Pattern.compile("(?i)(온라인|화상|zoom|줌|google\\s*meet|구글\\s*밋|구글밋|teams|팀즈|slack|슬랙|discord|디스코드)");
    private static final List<String> KNOWN_CLIENT_COMPANIES = List.of(
            "우아한형제들", "삼성전자", "현대카드", "오늘의집", "LG CNS",
            "에이블리", "무신사", "네이버", "카카오", "야놀자",
            "쿠팡", "토스", "배민", "라인", "당근", "롯데", "SKT"
    );
    private static final Set<String> COMPANY_STOP_WORDS = Set.of(
            "오늘", "내일", "모레", "이번", "다음", "프로젝트", "계약", "정산",
            "입금", "지급", "예산", "금액", "비용", "회의", "미팅", "작업",
            "외주", "예정", "팀", "장소", "주소", "온라인", "회의실", "카페",
            "SSAFY"
    );

    private final AiTaskParseClient aiTaskParseClient;
    private final Clock clock;

    public TaskParseService() {
        this(null, Clock.system(SEOUL_ZONE));
    }

    public TaskParseService(AiTaskParseClient aiTaskParseClient) {
        this(aiTaskParseClient, Clock.system(SEOUL_ZONE));
    }

    @Autowired
    public TaskParseService(AiTaskParseClient aiTaskParseClient, Clock clock) {
        this.aiTaskParseClient = aiTaskParseClient;
        this.clock = clock == null ? Clock.system(SEOUL_ZONE) : clock;
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
        aiResult.setCategoryId(null);
        aiResult.setContent(normalizeContent(aiResult.getContent()));
        normalizeDateTimeDetails(aiResult);
        normalizeAmountDetails(aiResult);

        MemoResult ruleMemo = extractMemo(sourceText);
        if (!StringUtils.hasText(aiResult.getMemo())) {
            aiResult.setMemo(ruleMemo.memo());
        } else {
            aiResult.setMemo(normalizeOptionalText(aiResult.getMemo()));
        }

        String clientCompany = normalizeOptionalText(aiResult.getClientCompany());
        if (clientCompany != null && !isValidClientCompanyCandidate(clientCompany)) {
            aiResult.setMemo(appendMemo(aiResult.getMemo(), formatPlaceMemo(clientCompany)));
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

    private void normalizeAmountDetails(TaskParseResponseDTO aiResult) {
        if (aiResult.getBudgetAmount() == null && aiResult.getBudget() != null) {
            aiResult.setBudgetAmount(aiResult.getBudget());
        }
        if (aiResult.getBudget() == null) {
            aiResult.setBudget(firstNonNullAmount(
                    aiResult.getBudgetAmount(),
                    aiResult.getContractAmount(),
                    aiResult.getPaidAmount(),
                    aiResult.getDepositAmount(),
                    aiResult.getBalanceAmount()
            ));
        }
        aiResult.setBudgetText(normalizeOptionalText(aiResult.getBudgetText()));
    }

    private Integer firstNonNullAmount(Integer... amounts) {
        for (Integer amount : amounts) {
            if (amount != null) {
                return amount;
            }
        }
        return null;
    }

    private void normalizeDateTimeDetails(TaskParseResponseDTO aiResult) {
        DateTimeDetail startDetail = normalizeDateTimeDetail(
                aiResult.getStartAt(),
                aiResult.getStartDate(),
                aiResult.getStartTime(),
                aiResult.getStartText(),
                aiResult.getStartPrecision()
        );
        aiResult.setStartAt(startDetail.at());
        aiResult.setStartDate(startDetail.date());
        aiResult.setStartTime(startDetail.time());
        aiResult.setStartText(startDetail.text());
        aiResult.setStartPrecision(startDetail.precision());

        DateTimeDetail endDetail = normalizeDateTimeDetail(
                aiResult.getEndAt(),
                aiResult.getEndDate(),
                aiResult.getEndTime(),
                aiResult.getEndText(),
                aiResult.getEndPrecision()
        );
        aiResult.setEndAt(endDetail.at());
        aiResult.setEndDate(endDetail.date());
        aiResult.setEndTime(endDetail.time());
        aiResult.setEndText(endDetail.text());
        aiResult.setEndPrecision(endDetail.precision());

        DateTimeDetail paidDetail = normalizeDateTimeDetail(
                aiResult.getPaidAt(),
                aiResult.getPaidDate(),
                aiResult.getPaidTime(),
                aiResult.getPaidText(),
                aiResult.getPaidPrecision()
        );
        aiResult.setPaidAt(paidDetail.at());
        aiResult.setPaidDate(paidDetail.date());
        aiResult.setPaidTime(paidDetail.time());
        aiResult.setPaidText(paidDetail.text());
        aiResult.setPaidPrecision(paidDetail.precision());

        if (startDetail.precision() == DateTimePrecision.TIME_ONLY && StringUtils.hasText(startDetail.text())) {
            aiResult.setMemo(appendMemo(aiResult.getMemo(), "시간만 명시됨: " + startDetail.text()));
        }
    }

    private TaskParseResponseDTO parseByRules(String sourceText) {
        List<String> warnings = new ArrayList<>();

        AmountDetails amountDetails = extractAmountDetails(sourceText, warnings);
        MemoResult memoResult = extractMemo(sourceText);
        memoResult = new MemoResult(
                appendMemo(memoResult.memo(), amountDetails.memo()),
                memoResult.parts()
        );
        TitleResult titleResult = extractTitle(sourceText, memoResult.parts());
        String sourceWithoutMemo = removeMemoParts(sourceText, memoResult.parts());
        String clientCompany = extractClientCompany(titleResult.title(), sourceWithoutMemo);

        LocalDate today = LocalDate.now(clock);
        List<DateCandidate> dateCandidates = extractDateCandidates(sourceText, today, warnings);
        List<TimeCandidate> timeCandidates = extractTimeCandidates(sourceText, warnings);
        PaidParseResult paidParseResult = extractPaidDetail(sourceText, today, dateCandidates, timeCandidates);
        DateCandidate paidDateCandidate = paidParseResult.dateCandidate();
        DateCandidate scheduleDateCandidate = chooseScheduleDateCandidate(dateCandidates, paidDateCandidate);
        ParsedTime parsedTime = extractTime(sourceText, warnings);
        DurationResult durationResult = extractDuration(sourceText, warnings);
        boolean paymentCenteredSentence = isPaymentCenteredSentence(sourceText);

        DateTimeDetail paidDetail = paidParseResult.detail();
        if (paidDetail.precision() != DateTimePrecision.NONE) {
            warnings.add("지급일로 보이는 날짜를 감지했습니다. 실제 지급일은 확인해 주세요.");
        }

        DateTimeDetail startDetail = DateTimeDetail.none();
        DateTimeDetail endDetail = DateTimeDetail.none();
        if (paymentCenteredSentence) {
            warnings.add("지급/입금 중심 문장으로 판단해 일정 시작일은 자동 확정하지 않았습니다.");
        } else if (scheduleDateCandidate != null && parsedTime.start() != null) {
            startDetail = DateTimeDetail.dateTime(
                    scheduleDateCandidate.date(),
                    parsedTime.start(),
                    joinDateTimeText(scheduleDateCandidate.expression(), parsedTime.startText())
            );
        } else if (scheduleDateCandidate != null) {
            startDetail = DateTimeDetail.dateOnly(scheduleDateCandidate.date(), scheduleDateCandidate.expression());
            warnings.add("시간 없이 날짜만 입력되어 일정 시작 일시는 확정하지 않고 날짜만 보존했습니다.");
        } else if (parsedTime.start() != null) {
            startDetail = DateTimeDetail.timeOnly(parsedTime.start(), parsedTime.startText());
            memoResult = new MemoResult(
                    appendMemo(memoResult.memo(), "시간만 명시됨: " + parsedTime.startText()),
                    memoResult.parts()
            );
            warnings.add("날짜 없이 시간만 입력되어 일정 시작 일시는 확정하지 않고 시간만 보존했습니다.");
        } else {
            warnings.add("일정 시작일을 확실히 인식하지 못했습니다.");
        }

        if (startDetail.precision() == DateTimePrecision.DATE_TIME && parsedTime.end() != null) {
            DateTimeDetail candidateEndDetail = DateTimeDetail.dateTime(
                    startDetail.date(),
                    parsedTime.end(),
                    joinDateTimeText(scheduleDateCandidate.expression(), parsedTime.endText())
            );
            if (candidateEndDetail.at().isBefore(startDetail.at())) {
                warnings.add("종료 시간이 시작 시간보다 앞서 종료 시간을 확정하지 않았습니다.");
            } else {
                endDetail = candidateEndDetail;
            }
        } else if (startDetail.precision() == DateTimePrecision.DATE_TIME && durationResult != null) {
            LocalDateTime calculatedEndAt = startDetail.at().plus(durationResult.duration());
            endDetail = DateTimeDetail.dateTime(
                    calculatedEndAt.toLocalDate(),
                    calculatedEndAt.toLocalTime(),
                    durationResult.expression()
            );
        } else if (startDetail.precision() == DateTimePrecision.TIME_ONLY && parsedTime.end() != null) {
            endDetail = DateTimeDetail.timeOnly(parsedTime.end(), parsedTime.endText());
        } else if (parsedTime.start() != null && scheduleDateCandidate != null) {
            warnings.add("종료 시간을 인식하지 못했습니다.");
        }

        memoResult = new MemoResult(
                appendMemo(memoResult.memo(), buildSecondaryDateMemo(
                        sourceText,
                        dateCandidates,
                        scheduleDateCandidate,
                        paidDateCandidate,
                        timeCandidates
                )),
                memoResult.parts()
        );

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
                endDetail.at() != null,
                amountDetails.primaryAmount() != null,
                paidDetail.precision() != DateTimePrecision.NONE,
                clientCompany != null,
                startDetail.at() != null,
                false
        );

        return TaskParseResponseDTO.builder()
                .sourceText(sourceText)
                .categoryId(null)
                .title(titleResult.title())
                .content(null)
                .memo(memoResult.memo())
                .startAt(startDetail.at())
                .startDate(startDetail.date())
                .startTime(startDetail.time())
                .startText(startDetail.text())
                .startPrecision(startDetail.precision())
                .endAt(endDetail.at())
                .endDate(endDetail.date())
                .endTime(endDetail.time())
                .endText(endDetail.text())
                .endPrecision(endDetail.precision())
                .status(TaskStatus.IN_PROGRESS)
                .clientCompany(clientCompany)
                .budget(amountDetails.primaryAmount())
                .budgetAmount(amountDetails.budgetAmount())
                .depositAmount(amountDetails.depositAmount())
                .paidAmount(amountDetails.paidAmount())
                .balanceAmount(amountDetails.balanceAmount())
                .contractAmount(amountDetails.contractAmount())
                .budgetText(amountDetails.budgetText())
                .paidAt(paidDetail.at())
                .paidDate(paidDetail.date())
                .paidTime(paidDetail.time())
                .paidText(paidDetail.text())
                .paidPrecision(paidDetail.precision())
                .confidence(confidence)
                .warnings(warnings)
                .build();
    }

    private MemoResult extractMemo(String sourceText) {
        List<String> memoParts = new ArrayList<>();
        List<String> placeParts = new ArrayList<>();
        List<String> onlineParts = new ArrayList<>();
        List<String> linkParts = new ArrayList<>();
        List<String> preparationParts = new ArrayList<>();
        List<String> extraParts = new ArrayList<>();

        Matcher placeMatcher = PLACE_BEFORE_PARTICLE_PATTERN.matcher(sourceText);
        while (placeMatcher.find()) {
            String place = cleanMemoCandidate(placeMatcher.group(1));
            addMemoPart(memoParts, place);
            addMemoPart(placeParts, formatPlaceMemo(place));
        }

        Matcher urlMatcher = URL_PATTERN.matcher(sourceText);
        while (urlMatcher.find()) {
            String url = cleanUrl(urlMatcher.group());
            String linkPhrase = findLinkPhrase(sourceText, urlMatcher.start(), urlMatcher.end());
            addMemoPart(memoParts, linkPhrase);
            addMemoPart(memoParts, url);
            addMemoPart(linkParts, formatMemoPart("링크", url));
        }

        Matcher onlineMatcher = ONLINE_MEMO_PATTERN.matcher(sourceText);
        while (onlineMatcher.find()) {
            String online = normalizeOptionalText(onlineMatcher.group());
            if (shouldRemoveOnlineFromTitle(online, sourceText, onlineMatcher.end())) {
                addMemoPart(memoParts, online);
            }
            addMemoPart(onlineParts, formatMemoPart("온라인", normalizeOnlineMemo(online)));
        }

        extractSupplementaryMemoParts(sourceText, memoParts, preparationParts, extraParts);

        List<String> displayParts = new ArrayList<>();
        displayParts.addAll(placeParts);
        displayParts.addAll(onlineParts);
        displayParts.addAll(linkParts);
        displayParts.addAll(preparationParts);
        displayParts.addAll(extraParts);

        if (displayParts.isEmpty()) {
            return new MemoResult(null, List.of());
        }
        return new MemoResult(String.join(", ", displayParts), List.copyOf(memoParts));
    }

    private void extractSupplementaryMemoParts(String sourceText,
                                               List<String> memoParts,
                                               List<String> preparationParts,
                                               List<String> extraParts) {
        for (String segment : sourceText.split("[,，;；]")) {
            String normalizedSegment = normalizeOptionalText(segment);
            if (normalizedSegment == null) {
                continue;
            }

            String cleanedSegment = cleanSupplementarySegment(normalizedSegment);
            PreparationMemo preparationMemo = extractPreparationMemo(cleanedSegment);
            if (preparationMemo != null) {
                addMemoPart(memoParts, normalizedSegment);
                addMemoPart(preparationParts, formatMemoPart(preparationMemo.label(), preparationMemo.value()));
                continue;
            }

            String extra = extractExtraMemo(cleanedSegment);
            if (extra != null) {
                addMemoPart(memoParts, normalizedSegment);
                addMemoPart(extraParts, extra);
            }
        }
    }

    private String cleanSupplementarySegment(String segment) {
        String cleanedSegment = removeStructuredExpressions(segment);
        cleanedSegment = URL_PATTERN.matcher(cleanedSegment).replaceAll(" ");
        cleanedSegment = ONLINE_MEMO_PATTERN.matcher(cleanedSegment).replaceAll(" ");
        cleanedSegment = cleanedSegment.replaceAll("\\s+", " ").trim();
        return normalizeOptionalText(cleanedSegment);
    }

    private PreparationMemo extractPreparationMemo(String segment) {
        String normalizedSegment = normalizeOptionalText(segment);
        if (normalizedSegment == null) {
            return null;
        }

        Matcher explicitPreparationMatcher = Pattern.compile("^(?:준비물|준비사항)\\s*[:：]?\\s*(.+)$")
                .matcher(normalizedSegment);
        if (explicitPreparationMatcher.find()) {
            return new PreparationMemo("준비물", normalizeListText(explicitPreparationMatcher.group(1)));
        }

        Matcher materialMatcher = Pattern.compile("^자료(?:는|은)?\\s+(.+\\s*준비)$")
                .matcher(normalizedSegment);
        if (materialMatcher.find()) {
            return new PreparationMemo("자료", materialMatcher.group(1).trim());
        }

        Matcher bringMatcher = Pattern.compile("^(.+?)\\s*지참$")
                .matcher(normalizedSegment);
        if (bringMatcher.find()) {
            return new PreparationMemo("준비물", normalizeListText(bringMatcher.group(1)));
        }

        Matcher preparationMatcher = Pattern.compile("^(.+?)\\s*준비$")
                .matcher(normalizedSegment);
        if (preparationMatcher.find()) {
            return new PreparationMemo("준비물", normalizeListText(preparationMatcher.group(1)));
        }

        return null;
    }

    private String extractExtraMemo(String segment) {
        String normalizedSegment = normalizeOptionalText(segment);
        if (normalizedSegment == null) {
            return null;
        }
        if (normalizedSegment.startsWith("사전 ") && normalizedSegment.contains("확인")) {
            return normalizedSegment;
        }
        return null;
    }

    private String normalizeListText(String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            return null;
        }
        return normalizedValue.replaceAll("\\s*(?:와|과|및)\\s*", ", ");
    }

    private String buildSecondaryDateMemo(String sourceText,
                                          List<DateCandidate> dateCandidates,
                                          DateCandidate scheduleDateCandidate,
                                          DateCandidate paidDateCandidate,
                                          List<TimeCandidate> timeCandidates) {
        if (dateCandidates.size() <= 1) {
            return null;
        }

        List<String> memoParts = new ArrayList<>();
        for (DateCandidate candidate : dateCandidates) {
            if (sameSpan(candidate, scheduleDateCandidate) || sameSpan(candidate, paidDateCandidate)) {
                continue;
            }

            String segment = sourceText.substring(
                    findDateSegmentStart(sourceText, candidate.start()),
                    findDateSegmentEnd(sourceText, candidate.end())
            );
            if (isPaymentDateSegment(segment)) {
                continue;
            }

            String label = extractSecondaryDateLabel(segment);
            if (label == null) {
                continue;
            }

            TimeCandidate timeCandidate = findTimeCandidateInDateSegment(sourceText, timeCandidates, candidate);
            addMemoPart(memoParts, label + ": " + formatDateForMemo(candidate.date(), timeCandidate));
        }
        return memoParts.isEmpty() ? null : String.join(", ", memoParts);
    }

    private String extractSecondaryDateLabel(String segment) {
        String label = removeStructuredExpressions(segment);
        label = label.replaceAll("\\b같은\\s*주\\b", " ");
        label = label.replaceAll("^(?:은|는|에|에는|까지)\\s*", "");
        label = label.replaceAll("\\s*(?:은|는|에|에는|까지)$", "");
        label = label.replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(label)) {
            return null;
        }
        if (segment.contains("까지") && !label.contains("마감")) {
            label = label + " 마감";
        }
        return normalizeOptionalText(label);
    }

    private TimeCandidate findTimeCandidateInDateSegment(String sourceText,
                                                         List<TimeCandidate> timeCandidates,
                                                         DateCandidate dateCandidate) {
        int segmentStart = findDateSegmentStart(sourceText, dateCandidate.start());
        int segmentEnd = findDateSegmentEnd(sourceText, dateCandidate.end());
        TimeCandidate selected = null;
        int selectedDistance = Integer.MAX_VALUE;
        for (TimeCandidate candidate : timeCandidates) {
            if (candidate.start() < segmentStart || candidate.end() > segmentEnd) {
                continue;
            }
            int distance = Math.abs(candidate.start() - dateCandidate.end());
            if (distance < selectedDistance) {
                selected = candidate;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    private String formatDateForMemo(LocalDate date, TimeCandidate timeCandidate) {
        if (timeCandidate == null) {
            return date.toString();
        }
        LocalTime time = timeCandidate.time();
        return "%sT%02d:%02d:%02d".formatted(date, time.getHour(), time.getMinute(), time.getSecond());
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

    private String formatPlaceMemo(String place) {
        return formatMemoPart("장소", place);
    }

    private String formatMemoPart(String label, String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            return null;
        }
        if (normalizedValue.startsWith(label + ":")) {
            return normalizedValue;
        }
        return label + ": " + normalizedValue;
    }

    private String normalizeOnlineMemo(String online) {
        String normalizedOnline = normalizeOptionalText(online);
        if (normalizedOnline == null) {
            return null;
        }
        String lowerOnline = normalizedOnline.toLowerCase();
        if (lowerOnline.contains("zoom") || normalizedOnline.contains("줌")) {
            return "Zoom";
        }
        if (lowerOnline.contains("google") || normalizedOnline.contains("구글")) {
            return "구글밋";
        }
        if (lowerOnline.contains("teams") || normalizedOnline.contains("팀즈")) {
            return "Teams";
        }
        if (lowerOnline.contains("slack") || normalizedOnline.contains("슬랙")) {
            return "Slack";
        }
        if (lowerOnline.contains("discord") || normalizedOnline.contains("디스코드")) {
            return "디스코드";
        }
        return normalizedOnline;
    }

    private String cleanUrl(String url) {
        String normalizedUrl = normalizeOptionalText(url);
        if (normalizedUrl == null) {
            return null;
        }
        return normalizedUrl.replaceAll("[).,，;；]+$", "");
    }

    private String findLinkPhrase(String sourceText, int urlStart, int urlEnd) {
        int phraseStartBoundary = Math.max(
                Math.max(sourceText.lastIndexOf(',', urlStart), sourceText.lastIndexOf('，', urlStart)),
                Math.max(sourceText.lastIndexOf(';', urlStart), sourceText.lastIndexOf('；', urlStart))
        );
        int phraseStart = phraseStartBoundary < 0 ? 0 : phraseStartBoundary + 1;
        String prefix = sourceText.substring(phraseStart, urlStart);
        int linkKeywordIndex = prefix.lastIndexOf("링크");
        if (linkKeywordIndex < 0) {
            return null;
        }
        return normalizeOptionalText(sourceText.substring(phraseStart + linkKeywordIndex, urlEnd));
    }

    private boolean shouldRemoveOnlineFromTitle(String online, String sourceText, int onlineEnd) {
        String normalizedOnline = normalizeOptionalText(online);
        if (normalizedOnline == null) {
            return false;
        }

        String lowerOnline = normalizedOnline.toLowerCase();
        if (lowerOnline.contains("zoom")
                || normalizedOnline.contains("줌")
                || lowerOnline.contains("google")
                || normalizedOnline.contains("구글")) {
            return true;
        }

        String afterOnline = sourceText.substring(onlineEnd);
        return afterOnline.matches("^\\s*(?:으로|로).*");
    }

    private String removeStructuredExpressions(String text) {
        String result = BUDGET_PATTERN.matcher(text).replaceAll(" ");
        result = ISO_DATE_PATTERN.matcher(result).replaceAll(" ");
        result = KOREAN_FULL_DATE_PATTERN.matcher(result).replaceAll(" ");
        result = MONTH_DAY_PATTERN.matcher(result).replaceAll(" ");
        result = SLASH_DATE_PATTERN.matcher(result).replaceAll(" ");
        result = NEXT_MONTH_DAY_PATTERN.matcher(result).replaceAll(" ");
        result = THIS_MONTH_END_PATTERN.matcher(result).replaceAll(" ");
        result = MONTH_END_PATTERN.matcher(result).replaceAll(" ");
        result = WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = DIRECT_WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = SAME_WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = MONTH_ORDINAL_WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = NEXT_MONTH_ORDINAL_WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = BARE_WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = RELATIVE_DATE_PATTERN.matcher(result).replaceAll(" ");
        result = KOREAN_FROM_TO_PATTERN.matcher(result).replaceAll(" ");
        result = CLOCK_RANGE_PATTERN.matcher(result).replaceAll(" ");
        result = SINGLE_TIME_PATTERN.matcher(result).replaceAll(" ");
        result = DURATION_PATTERN.matcher(result).replaceAll(" ");
        return result.replaceAll("\\s+", " ").trim();
    }

    private String removeMemoParts(String text, List<String> memoParts) {
        String result = text;
        List<String> sortedMemoParts = memoParts.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        for (String memoPart : sortedMemoParts) {
            result = Pattern.compile(Pattern.quote(memoPart) + "\\s*(?:에서의|에서|으로|로)?")
                    .matcher(result)
                    .replaceAll(" ");
        }
        return result.replaceAll("\\s+", " ").trim();
    }

    private String normalizeContent(String content) {
        return normalizeOptionalText(content);
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

    private String joinDateTimeText(String dateText, String timeText) {
        String normalizedDateText = normalizeOptionalText(dateText);
        String normalizedTimeText = normalizeOptionalText(timeText);
        if (normalizedDateText == null) {
            return normalizedTimeText;
        }
        if (normalizedTimeText == null) {
            return normalizedDateText;
        }
        return normalizedDateText + " " + normalizedTimeText;
    }

    private DateTimeDetail normalizeDateTimeDetail(LocalDateTime at,
                                                   LocalDate date,
                                                   LocalTime time,
                                                   String text,
                                                   DateTimePrecision precision) {
        DateTimePrecision normalizedPrecision = precision;
        LocalDate normalizedDate = date;
        LocalTime normalizedTime = time;

        if (at != null) {
            normalizedDate = at.toLocalDate();
            normalizedTime = at.toLocalTime();
            normalizedPrecision = DateTimePrecision.DATE_TIME;
        } else if (normalizedPrecision == null) {
            if (normalizedDate != null && normalizedTime != null) {
                normalizedPrecision = DateTimePrecision.DATE_TIME;
                at = LocalDateTime.of(normalizedDate, normalizedTime);
            } else if (normalizedDate != null) {
                normalizedPrecision = DateTimePrecision.DATE_ONLY;
            } else if (normalizedTime != null) {
                normalizedPrecision = DateTimePrecision.TIME_ONLY;
            } else {
                normalizedPrecision = DateTimePrecision.NONE;
            }
        }

        if (normalizedPrecision == DateTimePrecision.DATE_TIME && at == null && normalizedDate != null && normalizedTime != null) {
            at = LocalDateTime.of(normalizedDate, normalizedTime);
        }
        switch (normalizedPrecision) {
            case NONE -> {
                return DateTimeDetail.none();
            }
            case DATE_ONLY -> {
                return DateTimeDetail.dateOnly(normalizedDate, text);
            }
            case TIME_ONLY -> {
                return DateTimeDetail.timeOnly(normalizedTime, text);
            }
            case DATE_TIME -> {
                if (at != null) {
                    return new DateTimeDetail(
                            at,
                            at.toLocalDate(),
                            at.toLocalTime(),
                            normalizeOptionalText(text),
                            DateTimePrecision.DATE_TIME
                    );
                }
            }
        }
        return new DateTimeDetail(
                at,
                normalizedDate,
                normalizedTime,
                normalizeOptionalText(text),
                normalizedPrecision
        );
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

    private AmountDetails extractAmountDetails(String sourceText, List<String> warnings) {
        AmountDetailsBuilder builder = new AmountDetailsBuilder();

        Matcher rangeMatcher = AMOUNT_RANGE_PATTERN.matcher(sourceText);
        while (rangeMatcher.find()) {
            String rangeText = rangeMatcher.group(1).replace(",", "")
                    + "~"
                    + rangeMatcher.group(2).replace(",", "")
                    + rangeMatcher.group(3).replaceAll("\\s+", "");
            builder.budgetText(rangeText);
            builder.memo(appendMemo(builder.memo(), "예산 범위: " + rangeText));
        }

        List<String> percentMemoParts = new ArrayList<>();
        Matcher percentMatcher = PERCENT_PAYMENT_PATTERN.matcher(sourceText);
        while (percentMatcher.find()) {
            addMemoPart(percentMemoParts, normalizeOptionalText(percentMatcher.group()));
        }
        if (!percentMemoParts.isEmpty()) {
            builder.memo(appendMemo(builder.memo(), String.join(", ", percentMemoParts)));
        }

        Matcher amountMatcher = AMOUNT_PATTERN.matcher(sourceText);
        while (amountMatcher.find()) {
            if (isInsideRangeAmount(sourceText, amountMatcher.start(), amountMatcher.end())) {
                continue;
            }

            Integer amount = parseAmount(amountMatcher.group(1), amountMatcher.group(2), warnings);
            if (amount == null) {
                continue;
            }

            String context = amountContext(sourceText, amountMatcher.start(), amountMatcher.end());
            AmountRole role = classifyAmountRole(context);
            switch (role) {
                case BUDGET -> builder.budgetAmount(amount);
                case CONTRACT -> builder.contractAmount(amount);
                case DEPOSIT -> builder.depositAmount(amount);
                case BALANCE -> {
                    builder.balanceAmount(amount);
                    if (hasPaymentKeyword(context)) {
                        builder.paidAmount(amount);
                    }
                }
                case PAID -> builder.paidAmount(amount);
                case UNKNOWN -> builder.unknownAmount(amount);
            }

            if (shouldKeepAmountDetailInMemo(role, context)) {
                builder.memo(appendMemo(builder.memo(), normalizeAmountMemo(sourceText, amountMatcher.start(), amountMatcher.end())));
            }
        }

        return builder.build();
    }

    private boolean isInsideRangeAmount(String sourceText, int start, int end) {
        Matcher matcher = AMOUNT_RANGE_PATTERN.matcher(sourceText);
        while (matcher.find()) {
            if (start >= matcher.start() && end <= matcher.end()) {
                return true;
            }
        }
        return false;
    }

    private Integer parseAmount(String numberText, String unitText, List<String> warnings) {
        try {
            long amount = Long.parseLong(numberText.replace(",", ""));
            if ("만원".equals(unitText.replaceAll("\\s+", ""))) {
                amount = Math.multiplyExact(amount, 10000L);
            }
            if (amount > Integer.MAX_VALUE) {
                warnings.add("금액이 너무 커서 확정하지 못했습니다.");
                return null;
            }
            return (int) amount;
        } catch (ArithmeticException | NumberFormatException e) {
            warnings.add("금액을 인식하지 못했습니다.");
            return null;
        }
    }

    private String amountContext(String sourceText, int amountStart, int amountEnd) {
        int segmentStart = findSegmentStart(sourceText, amountStart);
        int segmentEnd = findSegmentEnd(sourceText, amountEnd);
        return sourceText.substring(segmentStart, segmentEnd);
    }

    private AmountRole classifyAmountRole(String context) {
        String normalizedContext = normalizeOptionalText(context);
        if (normalizedContext == null) {
            return AmountRole.UNKNOWN;
        }
        if (normalizedContext.matches(".*(?:전체\\s*예산|총예산|예산|견적).*")) {
            return AmountRole.BUDGET;
        }
        if (normalizedContext.matches(".*(?:계약\\s*총액|총액|계약금액).*")) {
            return AmountRole.CONTRACT;
        }
        if (normalizedContext.matches(".*(?:계약금|선금).*")) {
            return AmountRole.DEPOSIT;
        }
        if (normalizedContext.contains("잔금")) {
            return AmountRole.BALANCE;
        }
        if (normalizedContext.matches(".*(?:입금액|입금|지급|정산|송금|받기|지급받기|비용).*")) {
            return AmountRole.PAID;
        }
        return AmountRole.UNKNOWN;
    }

    private boolean hasPaymentKeyword(String context) {
        return normalizeOptionalText(context) != null && PAID_KEYWORD_PATTERN.matcher(context).find();
    }

    private boolean shouldKeepAmountDetailInMemo(AmountRole role, String context) {
        String normalizedContext = normalizeOptionalText(context);
        if (normalizedContext == null) {
            return false;
        }
        if (normalizedContext.contains("부가세 포함")) {
            return true;
        }
        if (role == AmountRole.BALANCE && hasPaymentKeyword(normalizedContext)) {
            return true;
        }
        return role == AmountRole.PAID && normalizedContext.contains("확인 필요");
    }

    private String normalizeAmountMemo(String sourceText, int amountStart, int amountEnd) {
        String segment = amountContext(sourceText, amountStart, amountEnd);
        return normalizeOptionalText(removeStructuredExpressionsKeepingAmount(segment));
    }

    private String removeStructuredExpressionsKeepingAmount(String text) {
        String result = ISO_DATE_PATTERN.matcher(text).replaceAll(" ");
        result = KOREAN_FULL_DATE_PATTERN.matcher(result).replaceAll(" ");
        result = MONTH_DAY_PATTERN.matcher(result).replaceAll(" ");
        result = SLASH_DATE_PATTERN.matcher(result).replaceAll(" ");
        result = NEXT_MONTH_DAY_PATTERN.matcher(result).replaceAll(" ");
        result = THIS_MONTH_END_PATTERN.matcher(result).replaceAll(" ");
        result = MONTH_END_PATTERN.matcher(result).replaceAll(" ");
        result = WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = DIRECT_WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = SAME_WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = MONTH_ORDINAL_WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = NEXT_MONTH_ORDINAL_WEEKDAY_PATTERN.matcher(result).replaceAll(" ");
        result = RELATIVE_DATE_PATTERN.matcher(result).replaceAll(" ");
        result = KOREAN_FROM_TO_PATTERN.matcher(result).replaceAll(" ");
        result = CLOCK_RANGE_PATTERN.matcher(result).replaceAll(" ");
        result = SINGLE_TIME_PATTERN.matcher(result).replaceAll(" ");
        result = DURATION_PATTERN.matcher(result).replaceAll(" ");
        return result.replaceAll("\\s+", " ").trim();
    }

    private TitleResult extractTitle(String sourceText, List<String> memoParts) {
        String firstLine = sourceText.split("\\R", 2)[0].trim();
        String titleSource = chooseTitleSource(firstLine);
        String title = BUDGET_PATTERN.matcher(titleSource).replaceAll("");
        title = ISO_DATE_PATTERN.matcher(title).replaceAll(" ");
        title = KOREAN_FULL_DATE_PATTERN.matcher(title).replaceAll(" ");
        title = MONTH_DAY_PATTERN.matcher(title).replaceAll(" ");
        title = SLASH_DATE_PATTERN.matcher(title).replaceAll(" ");
        title = NEXT_MONTH_DAY_PATTERN.matcher(title).replaceAll(" ");
        title = THIS_MONTH_END_PATTERN.matcher(title).replaceAll(" ");
        title = MONTH_END_PATTERN.matcher(title).replaceAll(" ");
        title = WEEKDAY_PATTERN.matcher(title).replaceAll(" ");
        title = DIRECT_WEEKDAY_PATTERN.matcher(title).replaceAll(" ");
        title = SAME_WEEKDAY_PATTERN.matcher(title).replaceAll(" ");
        title = MONTH_ORDINAL_WEEKDAY_PATTERN.matcher(title).replaceAll(" ");
        title = NEXT_MONTH_ORDINAL_WEEKDAY_PATTERN.matcher(title).replaceAll(" ");
        title = BARE_WEEKDAY_PATTERN.matcher(title).replaceAll(" ");
        title = RELATIVE_DATE_PATTERN.matcher(title).replaceAll(" ");
        title = KOREAN_FROM_TO_PATTERN.matcher(title).replaceAll(" ");
        title = CLOCK_RANGE_PATTERN.matcher(title).replaceAll(" ");
        title = SINGLE_TIME_PATTERN.matcher(title).replaceAll(" ");
        title = DURATION_PATTERN.matcher(title).replaceAll(" ");
        title = removeMemoParts(title, memoParts);
        title = title.replaceAll("[,，;；/]+", " ");
        title = title.replaceAll("\\s+", " ").trim();
        title = title.replaceAll("(계약금|선금|잔금|입금액)\\s*(?:은|는)", "$1").trim();
        title = title.replaceAll("\\s*(?:은|는)$", "").trim();
        title = title.replaceAll("^(?:에|에는|부터|까지)(?:\\s+|$)", "").trim();
        title = title.replaceAll("\\s+(?:에|에는|부터|까지|에서|으로|로)$", "").trim();
        title = title.replaceAll("(?:에서|에서의)$", "").trim();

        if (title.isEmpty()) {
            String fallback = firstLine.length() > 40 ? firstLine.substring(0, 40).trim() : firstLine;
            return new TitleResult(fallback, false);
        }
        return new TitleResult(title, true);
    }

    private String chooseTitleSource(String firstLine) {
        String[] segments = firstLine.split("[,，;；]|\\s*하고\\s*");
        if (segments.length <= 1) {
            return firstLine;
        }

        for (String segment : segments) {
            String normalizedSegment = normalizeOptionalText(segment);
            if (normalizedSegment != null && !isPaymentOrAmountOnlyTitleSegment(normalizedSegment)) {
                return normalizedSegment;
            }
        }
        return normalizeOptionalText(segments[0]) == null ? firstLine : normalizeOptionalText(segments[0]);
    }

    private boolean isPaymentOrAmountOnlyTitleSegment(String segment) {
        String normalizedSegment = normalizeOptionalText(segment);
        if (normalizedSegment == null) {
            return false;
        }
        boolean hasPaymentOrAmountKeyword = normalizedSegment.matches(".*(?:입금|지급|정산|송금|계약금|선금|잔금|입금액|예산|총액|견적).*");
        boolean hasScheduleActionKeyword = normalizedSegment.matches(".*(?:회의|미팅|인터뷰|논의|리뷰|발표|공유|착수|촬영|점검|제출|보고|전달|발송|수정|검토).*");
        return hasPaymentOrAmountKeyword && !hasScheduleActionKeyword;
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
                candidates.add(new DateCandidate(date, isoMatcher.start(), isoMatcher.end(), isoMatcher.group()));
            }
        }

        Matcher koreanFullDateMatcher = KOREAN_FULL_DATE_PATTERN.matcher(sourceText);
        while (koreanFullDateMatcher.find()) {
            LocalDate date = createDate(
                    parseInt(koreanFullDateMatcher.group(1)),
                    parseInt(koreanFullDateMatcher.group(2)),
                    parseInt(koreanFullDateMatcher.group(3)),
                    koreanFullDateMatcher.group(),
                    warnings
            );
            if (date != null) {
                candidates.add(new DateCandidate(date, koreanFullDateMatcher.start(), koreanFullDateMatcher.end(), koreanFullDateMatcher.group()));
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
                candidates.add(new DateCandidate(date, monthDayMatcher.start(), monthDayMatcher.end(), monthDayMatcher.group()));
            }
        }

        Matcher slashDateMatcher = SLASH_DATE_PATTERN.matcher(sourceText);
        while (slashDateMatcher.find()) {
            LocalDate date = resolveMonthDay(
                    parseInt(slashDateMatcher.group(1)),
                    parseInt(slashDateMatcher.group(2)),
                    today,
                    slashDateMatcher.group(),
                    warnings
            );
            if (date != null) {
                candidates.add(new DateCandidate(date, slashDateMatcher.start(), slashDateMatcher.end(), slashDateMatcher.group()));
            }
        }

        Matcher nextMonthDayMatcher = NEXT_MONTH_DAY_PATTERN.matcher(sourceText);
        while (nextMonthDayMatcher.find()) {
            LocalDate date = resolveNextMonthDay(
                    parseInt(nextMonthDayMatcher.group(1)),
                    today,
                    nextMonthDayMatcher.group(),
                    warnings
            );
            if (date != null) {
                candidates.add(new DateCandidate(date, nextMonthDayMatcher.start(), nextMonthDayMatcher.end(), nextMonthDayMatcher.group()));
            }
        }

        Matcher thisMonthEndMatcher = THIS_MONTH_END_PATTERN.matcher(sourceText);
        while (thisMonthEndMatcher.find()) {
            LocalDate date = today.withDayOfMonth(today.lengthOfMonth());
            candidates.add(new DateCandidate(date, thisMonthEndMatcher.start(), thisMonthEndMatcher.end(), thisMonthEndMatcher.group()));
        }

        Matcher monthEndMatcher = MONTH_END_PATTERN.matcher(sourceText);
        while (monthEndMatcher.find()) {
            LocalDate date = resolveMonthEnd(
                    parseInt(monthEndMatcher.group(1)),
                    today,
                    monthEndMatcher.group(),
                    warnings
            );
            if (date != null) {
                candidates.add(new DateCandidate(date, monthEndMatcher.start(), monthEndMatcher.end(), monthEndMatcher.group()));
            }
        }

        Matcher relativeMatcher = RELATIVE_DATE_PATTERN.matcher(sourceText);
        while (relativeMatcher.find()) {
            candidates.add(new DateCandidate(
                    resolveRelativeDate(relativeMatcher.group(), today),
                    relativeMatcher.start(),
                    relativeMatcher.end(),
                    relativeMatcher.group()
            ));
        }

        Matcher weekdayMatcher = WEEKDAY_PATTERN.matcher(sourceText);
        while (weekdayMatcher.find()) {
            LocalDate date = resolveWeekday(weekdayMatcher.group(1), weekdayMatcher.group(2), today);
            if (date.isBefore(today)) {
                warnings.add("이번 주 날짜가 이미 지난 날짜일 수 있습니다.");
            }
            candidates.add(new DateCandidate(date, weekdayMatcher.start(), weekdayMatcher.end(), weekdayMatcher.group()));
        }

        Matcher directWeekdayMatcher = DIRECT_WEEKDAY_PATTERN.matcher(sourceText);
        while (directWeekdayMatcher.find()) {
            LocalDate date = resolveWeekday(directWeekdayMatcher.group(1), directWeekdayMatcher.group(2), today);
            if (date.isBefore(today)) {
                warnings.add("이번 주 날짜가 이미 지난 날짜일 수 있습니다.");
            }
            candidates.add(new DateCandidate(date, directWeekdayMatcher.start(), directWeekdayMatcher.end(), directWeekdayMatcher.group()));
        }

        Matcher sameWeekdayMatcher = SAME_WEEKDAY_PATTERN.matcher(sourceText);
        while (sameWeekdayMatcher.find()) {
            LocalDate referenceDate = findPreviousDate(candidates, sameWeekdayMatcher.start(), today);
            LocalDate date = referenceDate
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusDays(toDayOfWeek(sameWeekdayMatcher.group(1)).getValue() - 1L);
            candidates.add(new DateCandidate(date, sameWeekdayMatcher.start(), sameWeekdayMatcher.end(), sameWeekdayMatcher.group()));
        }

        Matcher monthOrdinalWeekdayMatcher = MONTH_ORDINAL_WEEKDAY_PATTERN.matcher(sourceText);
        while (monthOrdinalWeekdayMatcher.find()) {
            LocalDate date = resolveMonthOrdinalWeekday(
                    parseInt(monthOrdinalWeekdayMatcher.group(1)),
                    monthOrdinalWeekdayMatcher.group(2),
                    monthOrdinalWeekdayMatcher.group(3),
                    today,
                    monthOrdinalWeekdayMatcher.group(),
                    warnings
            );
            if (date != null) {
                candidates.add(new DateCandidate(date, monthOrdinalWeekdayMatcher.start(), monthOrdinalWeekdayMatcher.end(), monthOrdinalWeekdayMatcher.group()));
            }
        }

        Matcher nextMonthOrdinalWeekdayMatcher = NEXT_MONTH_ORDINAL_WEEKDAY_PATTERN.matcher(sourceText);
        while (nextMonthOrdinalWeekdayMatcher.find()) {
            LocalDate nextMonth = today.plusMonths(1);
            LocalDate date = resolveOrdinalWeekday(
                    nextMonth.getYear(),
                    nextMonth.getMonthValue(),
                    nextMonthOrdinalWeekdayMatcher.group(1),
                    nextMonthOrdinalWeekdayMatcher.group(2),
                    nextMonthOrdinalWeekdayMatcher.group(),
                    warnings
            );
            if (date != null) {
                candidates.add(new DateCandidate(date, nextMonthOrdinalWeekdayMatcher.start(), nextMonthOrdinalWeekdayMatcher.end(), nextMonthOrdinalWeekdayMatcher.group()));
            }
        }

        Matcher bareWeekdayMatcher = BARE_WEEKDAY_PATTERN.matcher(sourceText);
        while (bareWeekdayMatcher.find()) {
            LocalDate date = resolveWeekday("이번", bareWeekdayMatcher.group(1), today);
            if (date.isBefore(today)) {
                date = date.plusWeeks(1);
            }
            candidates.add(new DateCandidate(date, bareWeekdayMatcher.start(), bareWeekdayMatcher.end(), bareWeekdayMatcher.group()));
        }

        candidates.sort(Comparator.comparingInt(DateCandidate::start)
                .thenComparing(Comparator.comparingInt(DateCandidate::end).reversed()));
        return removeOverlappingDateCandidates(candidates);
    }

    private List<DateCandidate> removeOverlappingDateCandidates(List<DateCandidate> candidates) {
        List<DateCandidate> filteredCandidates = new ArrayList<>();
        for (DateCandidate candidate : candidates) {
            if (filteredCandidates.isEmpty()) {
                filteredCandidates.add(candidate);
                continue;
            }

            DateCandidate previous = filteredCandidates.get(filteredCandidates.size() - 1);
            if (candidate.start() < previous.end()) {
                continue;
            }
            filteredCandidates.add(candidate);
        }
        return filteredCandidates;
    }

    private DateCandidate findPaidDateCandidate(String sourceText, List<DateCandidate> dateCandidates) {
        int firstPaymentKeywordIndex = findPaymentKeywordIndex(sourceText);
        for (int i = 0; i < dateCandidates.size(); i++) {
            DateCandidate candidate = dateCandidates.get(i);
            if (firstPaymentKeywordIndex >= 0
                    && candidate.end() < firstPaymentKeywordIndex
                    && hasDateCandidateAfter(dateCandidates, firstPaymentKeywordIndex)) {
                continue;
            }
            int nextDateStart = i + 1 < dateCandidates.size() ? dateCandidates.get(i + 1).start() : sourceText.length();
            int afterTo = Math.min(sourceText.length(), Math.min(candidate.end() + 24, nextDateStart));
            int beforeFrom = Math.max(0, candidate.start() - 12);
            String nearDate = sourceText.substring(beforeFrom, candidate.start())
                    + sourceText.substring(candidate.end(), afterTo);

            String segment = sourceText.substring(
                    findSegmentStart(sourceText, candidate.start()),
                    findSegmentEnd(sourceText, candidate.end())
            );
            if (PAID_KEYWORD_PATTERN.matcher(nearDate).find()
                    || isPaymentDateSegment(segment)
                    || (segment.contains("까지") && PAYMENT_CONTEXT_PATTERN.matcher(segment).find())) {
                return candidate;
            }
        }
        return null;
    }

    private boolean hasDateCandidateAfter(List<DateCandidate> dateCandidates, int index) {
        for (DateCandidate candidate : dateCandidates) {
            if (candidate.start() > index) {
                return true;
            }
        }
        return false;
    }

    private boolean isPaymentDateSegment(String segment) {
        String normalizedSegment = normalizeOptionalText(segment);
        return normalizedSegment != null
                && PAYMENT_CONTEXT_PATTERN.matcher(normalizedSegment).find()
                && !normalizedSegment.contains("입금액")
                && !normalizedSegment.matches(".*(?:회의|미팅|인터뷰|논의|리뷰|발표|공유|착수|촬영|점검|제출).*");
    }

    private boolean isPaymentCenteredSentence(String sourceText) {
        String normalizedSourceText = normalizeOptionalText(sourceText);
        if (normalizedSourceText == null || !PAYMENT_CONTEXT_PATTERN.matcher(normalizedSourceText).find()) {
            return false;
        }
        if (normalizedSourceText.contains("입금액")) {
            return false;
        }
        return !normalizedSourceText.matches(".*(?:회의|미팅|인터뷰|논의|리뷰|발표|공유|착수|촬영|점검|제출|보고|전달|종료|서명|오픈|시작).*")
                || normalizedSourceText.matches(".*(?:입금\\s*확인|입금\\s*예정|입금\\s*여부|지급받기|송금\\s*요청|잔금\\s*정산|세금계산서.*입금).*");
    }

    private PaidParseResult extractPaidDetail(String sourceText,
                                              LocalDate today,
                                              List<DateCandidate> dateCandidates,
                                              List<TimeCandidate> timeCandidates) {
        DateCandidate paidDateCandidate = findPaidDateCandidate(sourceText, dateCandidates);
        if (paidDateCandidate != null) {
            TimeCandidate paidTimeCandidate = findTimeCandidateInSegment(
                    sourceText,
                    timeCandidates,
                    paidDateCandidate.start(),
                    paidDateCandidate.end()
            );
            if (paidTimeCandidate != null) {
                return new PaidParseResult(
                        DateTimeDetail.dateTime(
                                paidDateCandidate.date(),
                                paidTimeCandidate.time(),
                                joinDateTimeText(paidDateCandidate.expression(), paidTimeCandidate.expression())
                        ),
                        paidDateCandidate
                );
            }
            return new PaidParseResult(
                    DateTimeDetail.dateOnly(paidDateCandidate.date(), paidDateCandidate.expression()),
                    paidDateCandidate
            );
        }

        LocalDate relativePaidDate = resolveRelativePaidDate(sourceText, today, dateCandidates);
        if (relativePaidDate != null) {
            int paymentKeywordIndex = findPaymentKeywordIndex(sourceText);
            TimeCandidate paidTimeCandidate = paymentKeywordIndex < 0
                    ? null
                    : findTimeCandidateInSegment(sourceText, timeCandidates, paymentKeywordIndex, paymentKeywordIndex);
            String dateText = PAID_SAME_DAY_PATTERN.matcher(sourceText).find() ? "당일" : "다음 날";
            if (paidTimeCandidate != null) {
                return new PaidParseResult(
                        DateTimeDetail.dateTime(
                                relativePaidDate,
                                paidTimeCandidate.time(),
                                joinDateTimeText(dateText, paidTimeCandidate.expression())
                        ),
                        null
                );
            }
            return new PaidParseResult(DateTimeDetail.dateOnly(relativePaidDate, dateText), null);
        }

        TimeCandidate paymentTimeOnly = findPaymentTimeOnly(sourceText, timeCandidates);
        if (paymentTimeOnly != null) {
            return new PaidParseResult(DateTimeDetail.timeOnly(paymentTimeOnly.time(), paymentTimeOnly.expression()), null);
        }

        return new PaidParseResult(DateTimeDetail.none(), null);
    }

    private TimeCandidate findTimeCandidateInSegment(String sourceText,
                                                     List<TimeCandidate> timeCandidates,
                                                     int anchorStart,
                                                     int anchorEnd) {
        int segmentStart = findSegmentStart(sourceText, anchorStart);
        int segmentEnd = findSegmentEnd(sourceText, anchorEnd);
        TimeCandidate selected = null;
        int selectedDistance = Integer.MAX_VALUE;
        for (TimeCandidate candidate : timeCandidates) {
            if (candidate.start() < segmentStart || candidate.end() > segmentEnd) {
                continue;
            }
            int distance = Math.min(Math.abs(candidate.start() - anchorStart), Math.abs(candidate.end() - anchorEnd));
            if (distance < selectedDistance) {
                selected = candidate;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    private LocalDate resolveRelativePaidDate(String sourceText, LocalDate today, List<DateCandidate> dateCandidates) {
        DateCandidate scheduleDateCandidate = dateCandidates.isEmpty() ? null : dateCandidates.get(0);
        if (scheduleDateCandidate != null && PAID_NEXT_DAY_PATTERN.matcher(sourceText).find()) {
            return scheduleDateCandidate.date().plusDays(1);
        }
        if (scheduleDateCandidate != null && PAID_SAME_DAY_PATTERN.matcher(sourceText).find()) {
            return scheduleDateCandidate.date();
        }
        if (isPaymentCenteredSentence(sourceText) && sourceText.contains("오늘")) {
            return today;
        }
        return null;
    }

    private TimeCandidate findPaymentTimeOnly(String sourceText, List<TimeCandidate> timeCandidates) {
        int paymentKeywordIndex = findPaymentKeywordIndex(sourceText);
        if (paymentKeywordIndex < 0) {
            return null;
        }
        return findTimeCandidateInSegment(sourceText, timeCandidates, paymentKeywordIndex, paymentKeywordIndex);
    }

    private int findPaymentKeywordIndex(String sourceText) {
        Matcher matcher = PAYMENT_CONTEXT_PATTERN.matcher(sourceText);
        return matcher.find() ? matcher.start() : -1;
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
                    isAmbiguous(start) || isAmbiguous(end),
                    buildTimeExpression(
                            fromToMatcher.group(1),
                            fromToMatcher.group(2),
                            null,
                            fromToMatcher.group(3)
                    ),
                    buildTimeExpression(
                            fromToMatcher.group(4),
                            fromToMatcher.group(5),
                            null,
                            fromToMatcher.group(6)
                    )
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
                    isAmbiguous(start) || isAmbiguous(end),
                    buildTimeExpression(
                            clockRangeMatcher.group(1),
                            clockRangeMatcher.group(2),
                            clockRangeMatcher.group(3),
                            clockRangeMatcher.group(4)
                    ),
                    buildTimeExpression(
                            clockRangeMatcher.group(5),
                            clockRangeMatcher.group(6),
                            clockRangeMatcher.group(7),
                            clockRangeMatcher.group(8)
                    )
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
                    isAmbiguous(start),
                    cleanTimeExpression(singleTimeMatcher.group()),
                    null
            );
        }

        return new ParsedTime(null, null, false, false, null, null);
    }

    private List<TimeCandidate> extractTimeCandidates(String sourceText, List<String> warnings) {
        List<TimeCandidate> candidates = new ArrayList<>();
        Matcher matcher = FLEXIBLE_TIME_PATTERN.matcher(sourceText);
        while (matcher.find()) {
            if (isTimeCandidatePartOfDate(sourceText, matcher.start(), matcher.end())) {
                continue;
            }

            TimeCandidate candidate = parseFlexibleTimeCandidate(
                    matcher.group(),
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    matcher.group(5),
                    matcher.start(),
                    matcher.end(),
                    warnings
            );
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private boolean isTimeCandidatePartOfDate(String sourceText, int start, int end) {
        int beforeStart = Math.max(0, start - 3);
        int afterEnd = Math.min(sourceText.length(), end + 2);
        String around = sourceText.substring(beforeStart, afterEnd);
        return around.matches(".*(?:월\\s*\\d{1,2}|\\d{1,2}\\s*일).*")
                && !around.contains("시")
                && !around.contains(":");
    }

    private TimeCandidate parseFlexibleTimeCandidate(String expression,
                                                     String noonText,
                                                     String meridiem,
                                                     String hourText,
                                                     String colonMinuteText,
                                                     String minuteText,
                                                     int start,
                                                     int end,
                                                     List<String> warnings) {
        if (noonText != null) {
            return new TimeCandidate(LocalTime.NOON, start, end, "정오");
        }

        try {
            int originalHour = Integer.parseInt(hourText);
            int minute = parseFlexibleMinute(colonMinuteText, minuteText);
            int hour = originalHour;
            String normalizedMeridiem = normalizeOptionalText(meridiem);

            if (minute < 0 || minute > 59) {
                warnings.add("분 단위 시간이 올바르지 않아 시간을 확정하지 못했습니다.");
                return null;
            }

            if ("오전".equals(normalizedMeridiem) || "아침".equals(normalizedMeridiem)) {
                if (hour == 12) {
                    hour = 0;
                }
            } else if ("오후".equals(normalizedMeridiem)
                    || "저녁".equals(normalizedMeridiem)
                    || "밤".equals(normalizedMeridiem)
                    || "퇴근 후".equals(normalizedMeridiem)) {
                if (hour < 12) {
                    hour += 12;
                }
            } else if ("새벽".equals(normalizedMeridiem)) {
                if (hour == 12) {
                    hour = 0;
                }
            }

            if (hour < 0 || hour > 23) {
                warnings.add("시간 표현을 인식하지 못했습니다.");
                return null;
            }

            return new TimeCandidate(LocalTime.of(hour, minute), start, end, cleanTimeExpression(expression));
        } catch (NumberFormatException | DateTimeException e) {
            warnings.add("시간 표현을 인식하지 못했습니다.");
            return null;
        }
    }

    private int parseFlexibleMinute(String colonMinuteText, String minuteText) {
        if (colonMinuteText != null) {
            return Integer.parseInt(colonMinuteText);
        }
        if (minuteText == null) {
            return 0;
        }
        String normalizedMinuteText = minuteText.replaceAll("\\s+", "");
        if ("반".equals(normalizedMinuteText)) {
            return 30;
        }
        return Integer.parseInt(normalizedMinuteText.replace("분", ""));
    }

    private String buildTimeExpression(String meridiem,
                                       String hourText,
                                       String colonMinuteText,
                                       String koreanMinuteText) {
        StringBuilder expression = new StringBuilder();
        if (StringUtils.hasText(meridiem)) {
            expression.append(meridiem).append(" ");
        }
        expression.append(hourText);
        if (colonMinuteText != null) {
            expression.append(":").append(colonMinuteText);
        } else {
            expression.append("시");
            if (koreanMinuteText != null) {
                expression.append(" ").append(koreanMinuteText).append("분");
            }
        }
        return cleanTimeExpression(expression.toString());
    }

    private String cleanTimeExpression(String expression) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        return expression.replaceAll("\\s*(?:에|에는|부터|까지)$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private DurationResult extractDuration(String sourceText, List<String> warnings) {
        Matcher matcher = DURATION_PATTERN.matcher(sourceText);
        if (!matcher.find()) {
            return null;
        }

        try {
            long hours = matcher.group(1) == null ? 0 : Long.parseLong(matcher.group(1));
            long minutes = 0;
            if (matcher.group(2) != null) {
                minutes = Long.parseLong(matcher.group(2));
            } else if (matcher.group(3) != null) {
                minutes = Long.parseLong(matcher.group(3));
            }

            Duration duration = Duration.ofHours(hours).plusMinutes(minutes);
            if (duration.isZero() || duration.isNegative()) {
                warnings.add("기간 표현을 인식하지 못했습니다.");
                return null;
            }
            return new DurationResult(duration, matcher.group().trim());
        } catch (NumberFormatException e) {
            warnings.add("기간 표현을 인식하지 못했습니다.");
            return null;
        }
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

    private LocalDate resolveNextMonthDay(int day, LocalDate today, String expression, List<String> warnings) {
        try {
            LocalDate nextMonth = today.plusMonths(1);
            return LocalDate.of(nextMonth.getYear(), nextMonth.getMonth(), day);
        } catch (DateTimeException e) {
            warnings.add("날짜 표현을 인식하지 못했습니다: " + expression);
            return null;
        }
    }

    private LocalDate resolveMonthEnd(int month, LocalDate today, String expression, List<String> warnings) {
        try {
            LocalDate date = LocalDate.of(today.getYear(), month, 1);
            if (date.isBefore(today.withDayOfMonth(1))) {
                date = date.plusYears(1);
                warnings.add("연도가 없는 월말 표현이 이미 지나 다음 해로 추정했습니다.");
            }
            return date.withDayOfMonth(date.lengthOfMonth());
        } catch (DateTimeException e) {
            warnings.add("날짜 표현을 인식하지 못했습니다: " + expression);
            return null;
        }
    }

    private LocalDate resolveMonthOrdinalWeekday(int month,
                                                 String ordinalText,
                                                 String dayText,
                                                 LocalDate today,
                                                 String expression,
                                                 List<String> warnings) {
        try {
            LocalDate baseDate = LocalDate.of(today.getYear(), month, 1);
            if (baseDate.isBefore(today.withDayOfMonth(1))) {
                baseDate = baseDate.plusYears(1);
                warnings.add("연도가 없는 날짜가 이미 지나 다음 해로 추정했습니다.");
            }
            return resolveOrdinalWeekday(
                    baseDate.getYear(),
                    baseDate.getMonthValue(),
                    ordinalText,
                    dayText,
                    expression,
                    warnings
            );
        } catch (DateTimeException e) {
            warnings.add("날짜 표현을 인식하지 못했습니다: " + expression);
            return null;
        }
    }

    private LocalDate resolveOrdinalWeekday(int year,
                                            int month,
                                            String ordinalText,
                                            String dayText,
                                            String expression,
                                            List<String> warnings) {
        try {
            LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
            LocalDate firstWeekday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(toDayOfWeek(dayText)));
            LocalDate date = firstWeekday.plusWeeks(toWeekIndex(ordinalText) - 1L);
            if (date.getMonthValue() != month) {
                warnings.add("월 내 n번째 요일 표현이 올바르지 않아 날짜를 확정하지 못했습니다: " + expression);
                return null;
            }
            return date;
        } catch (DateTimeException e) {
            warnings.add("날짜 표현을 인식하지 못했습니다: " + expression);
            return null;
        }
    }

    private int toWeekIndex(String ordinalText) {
        return switch (ordinalText) {
            case "첫째" -> 1;
            case "둘째" -> 2;
            case "셋째" -> 3;
            case "넷째" -> 4;
            default -> 5;
        };
    }

    private LocalDate findPreviousDate(List<DateCandidate> candidates, int beforeIndex, LocalDate today) {
        LocalDate previousDate = null;
        int previousStart = -1;
        for (DateCandidate candidate : candidates) {
            if (candidate.start() < beforeIndex && candidate.start() > previousStart) {
                previousDate = candidate.date();
                previousStart = candidate.start();
            }
        }
        return previousDate == null ? today : previousDate;
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

    private int findSegmentStart(String text, int index) {
        int comma = Math.max(text.lastIndexOf(',', index), text.lastIndexOf('，', index));
        int semicolon = Math.max(text.lastIndexOf(';', index), text.lastIndexOf('；', index));
        int segmentStart = Math.max(comma, semicolon);
        return segmentStart < 0 ? 0 : segmentStart + 1;
    }

    private int findSegmentEnd(String text, int index) {
        int comma = findNextOrLength(text, ',', index);
        comma = Math.min(comma, findNextOrLength(text, '，', index));
        int semicolon = findNextOrLength(text, ';', index);
        semicolon = Math.min(semicolon, findNextOrLength(text, '；', index));
        return Math.min(comma, semicolon);
    }

    private int findNextOrLength(String text, char character, int index) {
        int foundIndex = text.indexOf(character, index);
        return foundIndex < 0 ? text.length() : foundIndex;
    }

    private int findDateSegmentStart(String text, int index) {
        int segmentStart = findSegmentStart(text, index);
        int andIndex = text.lastIndexOf("하고", index);
        if (andIndex >= segmentStart) {
            return andIndex + "하고".length();
        }
        return segmentStart;
    }

    private int findDateSegmentEnd(String text, int index) {
        return findSegmentEnd(text, index);
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
        company = findCompany(PROJECT_COMPANY_PATTERN, normalizedTitle, 1);
        if (company != null) {
            return company;
        }
        company = findKnownClientCompany(normalizedSourceText);
        if (company != null) {
            return company;
        }
        return findKnownClientCompany(normalizedTitle);
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
                || normalizedCandidate.matches("\\d{4}\\s*년?")
                || COMPANY_STOP_WORDS.contains(normalizedCandidate)) {
            return false;
        }

        String lowerCandidate = normalizedCandidate.toLowerCase();
        return !normalizedCandidate.matches("\\d+\\s*층")
                && !normalizedCandidate.contains("주소")
                && !normalizedCandidate.matches(".*\\S역(?:\\s|$).*")
                && !isPlaceLikeText(normalizedCandidate)
                && !lowerCandidate.contains("online")
                && !lowerCandidate.contains("zoom")
                && !lowerCandidate.contains("meet")
                && !lowerCandidate.contains("teams")
                && !normalizedCandidate.contains("온라인")
                && !normalizedCandidate.contains("줌")
                && !normalizedCandidate.contains("구글 밋")
                && !normalizedCandidate.contains("팀즈");
    }

    private String findKnownClientCompany(String text) {
        String normalizedText = normalizeOptionalText(text);
        if (normalizedText == null) {
            return null;
        }

        for (String company : KNOWN_CLIENT_COMPANIES) {
            int searchIndex = 0;
            while (searchIndex < normalizedText.length()) {
                int foundIndex = normalizedText.indexOf(company, searchIndex);
                if (foundIndex < 0) {
                    break;
                }
                int endIndex = foundIndex + company.length();
                if (hasCompanyBoundary(normalizedText, foundIndex, endIndex)
                        && !isCompanyMentionInPlaceContext(normalizedText, foundIndex, endIndex)
                        && isValidClientCompanyCandidate(company)) {
                    return company;
                }
                searchIndex = endIndex;
            }
        }
        return null;
    }

    private boolean hasCompanyBoundary(String text, int startIndex, int endIndex) {
        boolean validStart = startIndex == 0 || !isNameCharacter(text.charAt(startIndex - 1));
        boolean validEnd = endIndex == text.length() || !isNameCharacter(text.charAt(endIndex));
        return validStart && validEnd;
    }

    private boolean isNameCharacter(char character) {
        return Character.isLetterOrDigit(character);
    }

    private boolean isCompanyMentionInPlaceContext(String text, int startIndex, int endIndex) {
        int contextStart = Math.max(0, startIndex - 16);
        int contextEnd = Math.min(text.length(), endIndex + 24);
        String context = text.substring(contextStart, contextEnd);
        return isPlaceLikeText(context);
    }

    private boolean isPlaceLikeText(String text) {
        String normalizedText = normalizeOptionalText(text);
        return normalizedText != null && PLACE_CONTEXT_PATTERN.matcher(normalizedText).find();
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

    private record PreparationMemo(String label, String value) {
    }

    private enum AmountRole {
        BUDGET,
        CONTRACT,
        DEPOSIT,
        BALANCE,
        PAID,
        UNKNOWN
    }

    private record AmountDetails(Integer budgetAmount,
                                 Integer depositAmount,
                                 Integer paidAmount,
                                 Integer balanceAmount,
                                 Integer contractAmount,
                                 String budgetText,
                                 String memo) {

        private Integer primaryAmount() {
            if (budgetAmount != null) {
                return budgetAmount;
            }
            if (contractAmount != null) {
                return contractAmount;
            }
            if (paidAmount != null) {
                return paidAmount;
            }
            if (depositAmount != null) {
                return depositAmount;
            }
            return balanceAmount;
        }
    }

    private static final class AmountDetailsBuilder {
        private Integer budgetAmount;
        private Integer depositAmount;
        private Integer paidAmount;
        private Integer balanceAmount;
        private Integer contractAmount;
        private Integer unknownAmount;
        private String budgetText;
        private String memo;

        private void budgetAmount(Integer amount) {
            if (budgetAmount == null) {
                budgetAmount = amount;
            }
        }

        private void depositAmount(Integer amount) {
            if (depositAmount == null) {
                depositAmount = amount;
            }
        }

        private void paidAmount(Integer amount) {
            if (paidAmount == null) {
                paidAmount = amount;
            }
        }

        private void balanceAmount(Integer amount) {
            if (balanceAmount == null) {
                balanceAmount = amount;
            }
        }

        private void contractAmount(Integer amount) {
            if (contractAmount == null) {
                contractAmount = amount;
            }
        }

        private void unknownAmount(Integer amount) {
            if (unknownAmount == null) {
                unknownAmount = amount;
            }
        }

        private void budgetText(String text) {
            if (budgetText == null) {
                budgetText = text;
            }
        }

        private String memo() {
            return memo;
        }

        private void memo(String memo) {
            this.memo = memo;
        }

        private AmountDetails build() {
            Integer resolvedPaidAmount = paidAmount;
            if (resolvedPaidAmount == null) {
                resolvedPaidAmount = unknownAmount;
            }
            return new AmountDetails(
                    budgetAmount,
                    depositAmount,
                    resolvedPaidAmount,
                    balanceAmount,
                    contractAmount,
                    budgetText,
                    memo
            );
        }
    }

    private record DateCandidate(LocalDate date, int start, int end, String expression) {
    }

    private record TimeCandidate(LocalTime time, int start, int end, String expression) {
    }

    private record PaidParseResult(DateTimeDetail detail, DateCandidate dateCandidate) {
    }

    private record ParsedTime(LocalTime start,
                              LocalTime end,
                              boolean found,
                              boolean ambiguousMeridiem,
                              String startText,
                              String endText) {
    }

    private record TimeToken(LocalTime time, boolean ambiguousMeridiem) {
    }

    private record DurationResult(Duration duration, String expression) {
    }

    private record DateTimeDetail(LocalDateTime at,
                                  LocalDate date,
                                  LocalTime time,
                                  String text,
                                  DateTimePrecision precision) {

        private static DateTimeDetail none() {
            return new DateTimeDetail(null, null, null, null, DateTimePrecision.NONE);
        }

        private static DateTimeDetail dateOnly(LocalDate date, String text) {
            return new DateTimeDetail(null, date, null, text, DateTimePrecision.DATE_ONLY);
        }

        private static DateTimeDetail timeOnly(LocalTime time, String text) {
            return new DateTimeDetail(null, null, time, text, DateTimePrecision.TIME_ONLY);
        }

        private static DateTimeDetail dateTime(LocalDate date, LocalTime time, String text) {
            return new DateTimeDetail(
                    LocalDateTime.of(date, time),
                    date,
                    time,
                    text,
                    DateTimePrecision.DATE_TIME
            );
        }
    }
}
