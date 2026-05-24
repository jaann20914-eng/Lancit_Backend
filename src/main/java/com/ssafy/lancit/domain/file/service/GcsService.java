package com.ssafy.lancit.domain.file.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.ssafy.lancit.domain.file.event.FileDeleteEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcsService {
 
    private final Storage storage;
 
    @Value("${gcs.bucket-name}")
    private String bucketName;
 
    /** GCS 파일 업로드 → 업로드된 파일 경로(gs://...) 반환 */
    public String upload(MultipartFile file) throws IOException {
        // TODO 지원: UUID 기반 파일명 생성 → BlobInfo 생성 → storage.create() 호출
        String sysName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        BlobId blobId = BlobId.of(bucketName, sysName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());
        return sysName;
    }
 
    /** GCS 파일 삭제
     *  @TransactionalEventListener → 트랜잭션 커밋 이후에만 실행
     *  (DB 삭제 롤백 시 GCS 삭제 안 됨) */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileDelete(FileDeleteEvent event) {
        try {
            storage.delete(BlobId.of(bucketName, event.getUploadPath()));
            log.info("[GCS] 파일 삭제 완료: {}", event.getUploadPath());
        } catch (Exception e) {
            log.error("[GCS] 파일 삭제 실패: {} - {}", event.getUploadPath(), e.getMessage());
            // TODO 지원: 실패 시 재시도 or 알림 처리
        }
    }
}