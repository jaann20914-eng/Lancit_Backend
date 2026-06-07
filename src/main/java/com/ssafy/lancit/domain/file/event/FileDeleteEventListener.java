package com.ssafy.lancit.domain.file.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ssafy.lancit.domain.file.mapper.FileDeleteQueueMapper;
import com.ssafy.lancit.domain.file.service.GcsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDeleteEventListener {

    private final GcsService gcsService;
    private final FileDeleteQueueMapper fileDeleteQueueMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    
    public void handleFileDelete(FileDeleteEvent event) {
        try {
        	gcsService.deleteByPath(event.getUploadPath());
        } catch (Exception e) {
            log.error("GCS 파일 삭제 실패 : {}",event.getUploadPath(),e);
            // file_delete_queue 저장
            fileDeleteQueueMapper.insert(event.getUploadPath());
        }
    }
}