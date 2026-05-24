package com.ssafy.lancit.domain.calendar.task.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.OwnerType;
import com.ssafy.lancit.global.enums.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskDTO {
    private int taskId;
    private String email;
    private OwnerType ownerType;
    private int categoryId;
    private String title;
    private String content;
    private TaskStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String clientCompany;
    private int budget;
    private LocalDateTime paidAt;
    private boolean autoRegistered;
    private String autoRegisteredSource;
}