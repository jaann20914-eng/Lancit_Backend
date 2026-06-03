package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.global.enums.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TaskParseService {

    private static final Pattern BUDGET_PATTERN =
            Pattern.compile("(?:예산|금액|비용)?\\s*[:：]?\\s*([0-9,]+)\\s*(만원|원)");
    private static final Pattern LEADING_DATE_TIME_PATTERN =
            Pattern.compile("^\\s*\\d{1,2}\\s*월\\s*\\d{1,2}\\s*일\\s*(오전|오후)?\\s*\\d{1,2}?\\s*시?(\\s*\\d{1,2}\\s*분)?\\s*(까지|에|부터)?\\s*");

    public TaskParseResponseDTO parse(TaskParseRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getSourceText() == null || requestDTO.getSourceText().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        String sourceText = requestDTO.getSourceText().trim();
        List<String> warnings = new ArrayList<>();
        Integer budget = extractBudget(sourceText, warnings);
        String title = extractTitle(sourceText);

        warnings.add("카테고리를 자동으로 선택하지 못했습니다.");
        warnings.add("정확한 시작일과 종료일은 사용자가 확인해야 합니다.");
        warnings.add("회사명을 인식하지 못했습니다.");

        double confidence = budget == null ? 0.4 : 0.5;

        return TaskParseResponseDTO.builder()
                .sourceText(sourceText)
                .categoryId(null)
                .title(title)
                .content(sourceText)
                .startAt(null)
                .endAt(null)
                .status(TaskStatus.IN_PROGRESS)
                .clientCompany(null)
                .budget(budget)
                .paidAt(null)
                .confidence(confidence)
                .warnings(warnings)
                .build();
    }

    private Integer extractBudget(String sourceText, List<String> warnings) {
        Matcher matcher = BUDGET_PATTERN.matcher(sourceText);
        if (!matcher.find()) {
            return null;
        }

        try {
            int amount = Integer.parseInt(matcher.group(1).replace(",", ""));
            if ("만원".equals(matcher.group(2))) {
                return Math.multiplyExact(amount, 10000);
            }
            return amount;
        } catch (ArithmeticException | NumberFormatException e) {
            warnings.add("금액을 인식하지 못했습니다.");
            return null;
        }
    }

    private String extractTitle(String sourceText) {
        String firstLine = sourceText.split("\\R", 2)[0].trim();
        String title = BUDGET_PATTERN.matcher(firstLine).replaceAll("");
        title = LEADING_DATE_TIME_PATTERN.matcher(title).replaceFirst("");
        title = title.replaceAll("^[,\\s]+|[,\\s]+$", "").trim();

        if (title.isEmpty()) {
            return firstLine;
        }
        return title;
    }
}
