package com.ssafy.lancit.domain.calendar.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.ssafy.lancit.global.enums.DateTimePrecision;
import com.ssafy.lancit.global.enums.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskParseResponseDTO {
    private String sourceText;
    private Integer categoryId;
    private String title;
    private String content;
    private String memo;
    private LocalDateTime startAt;
    private LocalDate startDate;
    private LocalTime startTime;
    private String startText;
    private DateTimePrecision startPrecision;
    private LocalDateTime endAt;
    private LocalDate endDate;
    private LocalTime endTime;
    private String endText;
    private DateTimePrecision endPrecision;
    private TaskStatus status;
    private String clientCompany;
    private Integer budget;
    private Integer budgetAmount;
    private Integer depositAmount;
    private Integer paidAmount;
    private Integer balanceAmount;
    private Integer contractAmount;
    private String budgetText;
    private LocalDateTime paidAt;
    private LocalDate paidDate;
    private LocalTime paidTime;
    private String paidText;
    private DateTimePrecision paidPrecision;
    private Double confidence;
    private List<String> warnings;
}
