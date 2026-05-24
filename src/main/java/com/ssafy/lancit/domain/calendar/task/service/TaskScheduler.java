package com.ssafy.lancit.domain.calendar.task.service;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskScheduler {
 
    private final TaskMapper taskMapper;
 
    /** CAL-11 납기일 D-7, D-3, D-1 알림
     *  매일 오전 9시 실행 */
    @Scheduled(cron = "0 0 9 * * *")
    public void notifyDeadlines() {
        // TODO 영은: taskMapper.findDeadlineTasks(7) → 알림 발송 (FCM or 이메일)
        //   taskMapper.findDeadlineTasks(3)
        //   taskMapper.findDeadlineTasks(1)
        log.info("[Scheduler] 납기일 알림 실행");
    }
}
 