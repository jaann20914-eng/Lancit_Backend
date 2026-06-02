package com.ssafy.lancit.domain.calendar.task.service;

import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskScheduler {

    private final TaskMapper taskMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate; // ★ String으로 변경

    private static final String NOTIFIED_PREFIX = "notified:task:";

    @Scheduled(cron = "0 0 9 * * *")
    public void notifyDeadlines() {
        log.info("[Scheduler] 납기일 알림 실행 - {}", LocalDate.now());

        for (int days : new int[]{7, 3, 1, 0}) {
            List<TaskDTO> tasks = taskMapper.findDeadlineTasks(days);
            for (TaskDTO task : tasks) {
                sendIfNotSent(task, days);
            }
        }
    }

    private void sendIfNotSent(TaskDTO task, int daysLeft) {
        String key = NOTIFIED_PREFIX + task.getTaskId() + ":" + LocalDate.now();

        String alreadySent = redisTemplate.opsForValue().get(key);
        if (alreadySent != null) {
            log.debug("[Scheduler] 이미 발송된 알림 스킵 - taskId: {}", task.getTaskId());
            return;
        }

        String message = buildMessage(task, daysLeft);
        messagingTemplate.convertAndSend(
                "/sub/notification/" + task.getEmail(),
                message
        );

        redisTemplate.opsForValue().set(key, "sent", 2, TimeUnit.DAYS);

        log.info("[Scheduler] 알림 발송 - taskId: {}, email: {}, D-{}",
                task.getTaskId(), task.getEmail(), daysLeft);
    }

    private String buildMessage(TaskDTO task, int daysLeft) {
        if (daysLeft == 0) {
            return String.format("[LANCIT] '%s' 납기일이 오늘입니다!", task.getTitle());
        }
        return String.format("[LANCIT] '%s' 납기일이 %d일 남았습니다.", task.getTitle(), daysLeft);
    }
}