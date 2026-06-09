package com.ssafy.lancit.domain.calendar.task.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseRequestDTO;
import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.calendar.task.service.TaskParseService;
import com.ssafy.lancit.domain.calendar.task.service.TaskService;
import com.ssafy.lancit.global.enums.OwnerType;
import com.ssafy.lancit.global.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskControllerTest {

    @Test
    void parseTaskReturnsPreviewWithoutSavingTask() {
        String sourceText = "내일 오후 3시에 SSAFY 1층 회의실에서 팀 회의";
        TaskParseRequestDTO request = TaskParseRequestDTO.builder()
                .sourceText(sourceText)
                .build();
        TaskParseResponseDTO preview = TaskParseResponseDTO.builder()
                .sourceText(sourceText)
                .categoryId(null)
                .title("팀 회의")
                .content(null)
                .memo("SSAFY 1층 회의실")
                .startAt(LocalDateTime.of(2026, 6, 11, 15, 0))
                .endAt(null)
                .status(TaskStatus.IN_PROGRESS)
                .clientCompany(null)
                .budget(null)
                .paidAt(null)
                .confidence(0.85)
                .warnings(List.of())
                .build();
        RecordingTaskParseService taskParseService = new RecordingTaskParseService(preview);
        RecordingTaskService taskService = new RecordingTaskService();
        TaskController taskController = new TaskController(taskService, taskParseService);

        ResponseEntity<ApiResponse<TaskParseResponseDTO>> response = taskController.parseTask(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(preview);
        assertThat(taskParseService.parsedRequest).isSameAs(request);
        assertThat(taskService.saveAttempted).isFalse();
    }

    private static class RecordingTaskParseService extends TaskParseService {
        private final TaskParseResponseDTO response;
        private TaskParseRequestDTO parsedRequest;

        private RecordingTaskParseService(TaskParseResponseDTO response) {
            this.response = response;
        }

        @Override
        public TaskParseResponseDTO parse(TaskParseRequestDTO requestDTO) {
            this.parsedRequest = requestDTO;
            return response;
        }
    }

    private static class RecordingTaskService extends TaskService {
        private boolean saveAttempted;

        private RecordingTaskService() {
            super((TaskMapper) null, (CategoryMapper) null);
        }

        @Override
        public void create(TaskDTO dto, String email, OwnerType ownerType) {
            saveAttempted = true;
        }

        @Override
        public void createFromParsed(TaskDTO dto, String email, OwnerType ownerType) {
            saveAttempted = true;
        }
    }
}
