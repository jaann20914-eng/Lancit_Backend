package com.ssafy.lancit.domain.file.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.domain.file.dto.FileDeleteQueue;
import com.ssafy.lancit.domain.file.mapper.FileDeleteQueueMapper;
import com.ssafy.lancit.domain.file.service.GcsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDeleteRetryScheduler {
	
	private final FileDeleteQueueMapper fileDeleteQueueMapper;
	private final GcsService gcsService;
	
	@Scheduled(cron = "0 0 2 * * *")
	public void retryDeleteFiles() {
		List<FileDeleteQueue> list = fileDeleteQueueMapper.findAll();
		for(FileDeleteQueue dto : list) {
			
			//retry_count >= 3 이면 로그만 남기고 포기
			if (dto.getRetryCount() >= 3) {
			    log.error("GCS 삭제 최대 재시도 초과 - path: {}", dto.getUploadPath());
			    continue; // 스킵
			}
			
			try {
				// 성공 → deleteByPathOrThrow(id)
				gcsService.deleteByPathOrThrow(dto.getUploadPath());
				fileDeleteQueueMapper.delete(dto.getFileId());
			} catch (Exception e) {
				//실패 → incrementRetryCount(id) 
				fileDeleteQueueMapper.incrementRetryCount(dto.getFileId());
				e.printStackTrace();
			}
		}

      
    }
	
}
 