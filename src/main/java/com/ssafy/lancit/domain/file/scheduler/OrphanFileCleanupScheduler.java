package com.ssafy.lancit.domain.file.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupScheduler {

    private final FileMapper fileMapper;
    private final FileService fileService;

    /**
     * 계약 PDF 생성 중 트랜잭션 실패로 발생한
     * 고아 CONTRACT 파일 정리
     */
    @Scheduled(cron = "0 30 2 * * *")
    public void cleanupOrphanContractFiles() {

        List<FileDTO> orphans =
                fileMapper.findOrphanContractFiles();

        for (FileDTO file : orphans) {
            try {
                fileService.delete(file.getFileId());

                log.info(
                    "고아 계약 파일 정리 완료. fileId={}",
                    file.getFileId()
                );

            } catch (Exception e) {

                log.error(
                    "고아 계약 파일 정리 실패. fileId={}",
                    file.getFileId(),
                    e
                );
            }
        }
    }
}