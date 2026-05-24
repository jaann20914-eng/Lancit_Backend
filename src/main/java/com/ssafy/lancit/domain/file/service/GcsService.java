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

    /**
     * GCS 파일 업로드
     * 반환값: sysName → DB uploadPath 컬럼에 저장
     *
     * TODO 지원 [1]: application.properties gcs.bucket-name 실제 버킷명 입력
     * TODO 지원 [2]: GCS 버킷 allUsers에 Storage Object Viewer 권한 부여 확인
     */
    public String upload(MultipartFile file) throws IOException {
        String sysName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        BlobId blobId = BlobId.of(bucketName, sysName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());
        return sysName;
    }

    /**
     * sysName → 공개 URL 변환
     * FileService.upload() 에서 uploadPath 저장 전 호출
     * 반환 형식: https://storage.googleapis.com/{bucketName}/{sysName}
     */
    public String getPublicUrl(String sysName) {
        return "https://storage.googleapis.com/" + bucketName + "/" + sysName;
    }

    /**
     * GCS 파일 삭제
     * DB 커밋 성공 후에만 실행 → DB 롤백 시 GCS 파일 유지됨
     *
     * TODO 지원 [1]: 삭제 실패 정책 팀과 결정
     *               - 현재는 로그만 남기고 넘어감 (파일 고아 허용)
     *               - 엄격하게 할 경우 재시도 3회 후 슬랙/메일 알림
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileDelete(FileDeleteEvent event) {
        try {
            storage.delete(BlobId.of(bucketName, event.getUploadPath()));
        } catch (Exception e) {
            // TODO 지원 [1]: 실패 정책 결정 후 구현
            log.error("[GCS] 삭제 실패: {} - {}", event.getUploadPath(), e.getMessage());
        }
    }
}