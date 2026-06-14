package com.ssafy.lancit.domain.calendar.task.service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.domain.calendar.task.dto.TaskDTO;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.notification.dto.NotificationDTO;
import com.ssafy.lancit.domain.notification.websocket.NotificationStompPublisher;
import com.ssafy.lancit.global.enums.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskScheduler {

    private final TaskMapper taskMapper;
    private final NotificationStompPublisher notificationStompPublisher;
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

        NotificationDTO notification = NotificationDTO.builder()
                .receiverEmail(task.getEmail())
                .type(NotificationType.CHAT) // 추후 CALENDAR 타입 추가 시 변경
                .targetId(task.getTaskId())
                .message(buildMessage(task, daysLeft)) // 캘린더 전용 메시지
                .build();

        notificationStompPublisher.publish(notification);

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