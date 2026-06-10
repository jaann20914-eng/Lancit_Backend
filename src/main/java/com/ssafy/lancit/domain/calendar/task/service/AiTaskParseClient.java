package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.domain.calendar.task.dto.TaskParseResponseDTO;

public interface AiTaskParseClient {

    TaskParseResponseDTO parse(String sourceText);
}
