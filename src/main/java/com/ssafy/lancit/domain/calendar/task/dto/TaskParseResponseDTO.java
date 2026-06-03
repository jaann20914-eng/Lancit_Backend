package com.ssafy.lancit.domain.calendar.task.dto;

import java.time.LocalDateTime;
import java.util.List;

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
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private TaskStatus status;
    private String clientCompany;
    private Integer budget;
    private LocalDateTime paidAt;
    private Double confidence;
    private List<String> warnings;
}
