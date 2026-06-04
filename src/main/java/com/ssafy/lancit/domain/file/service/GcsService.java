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
import com.ssafy.lancit.global.enums.FileParentType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcsService {

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;
    
    
    
    // gcs 폴더매핑해주는 메서드
    private String getFolder(FileParentType parentType) {
        return switch (parentType) {
            case PROFILE           -> "profile/";
            case PORTFOLIO_BANNER  -> "portfolio/banner/";
            case PORTFOLIO_FILE    -> "portfolio/file/";
            case CONTRACT          -> "contract/";
            case CHAT              -> "chat/";
        };
    }

    // GCS 파일 업로드
    public String upload(MultipartFile file, FileParentType parentType) throws IOException {
        String folder = getFolder(parentType);
        String sysName = folder + UUID.randomUUID() + "_" + file.getOriginalFilename();
        BlobId blobId = BlobId.of(bucketName, sysName);
        
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());
        return sysName;
    }

    /**
     * ★ GCS 파일 직접 삭제 (업로드 후 DB 실패 시 롤백용)
     * FileService.upload() 의 catch 블록에서 호출
     */
    public void deleteByPath(String sysName) {
        try {
            storage.delete(BlobId.of(bucketName, sysName));
            log.info("[GCS] 롤백 삭제 완료: {}", sysName);
        } catch (Exception e) {
            log.error("[GCS] 롤백 삭제 실패: {} - {}", sysName, e.getMessage());
        }
    }

    /**
     * GCS 파일 삭제 (이벤트 리스너)
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
            log.info("[GCS] 삭제 완료: {}", event.getUploadPath());
        } catch (Exception e) {
            log.error("[GCS] 삭제 실패: {} - {}", event.getUploadPath(), e.getMessage());
        }
    }
}