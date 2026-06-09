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
            case TEMP              -> "temp/";
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

    // 롤백용 (예외 잡음) + GCS 파일 리스너 안거치고 직접 삭제 : 업로드중 롤백전용 콜금지
    // FileService.upload() 의 catch 블록에서 호출
    public void deleteByPath(String sysName) {
        try {
            storage.delete(BlobId.of(bucketName, sysName));
            log.info("[GCS] 롤백 삭제 완료: {}", sysName);
        } catch (Exception e) {
            log.error("[GCS] 롤백 삭제 실패: {} - {}", sysName, e.getMessage());
        }
    }
	// 배치용 (예외 던짐) + GCS 파일 리스너 안거치고 직접 삭제
    // FileDeleteRetryScheduler.retryDeleteFiles() 에서 사용
	    public void deleteByPathOrThrow(String sysName) throws Exception {
	        storage.delete(BlobId.of(bucketName, sysName));
	        log.info("[GCS] 배치 삭제 완료: {}", sysName);
	    }
	    
	    
	 // temp -> 새로운 타입으로 이동하기
	    public String move(String oldPath, FileParentType newType) {
	        String fileName = oldPath.substring(oldPath.lastIndexOf("/") + 1);
	        String newPath = getFolder(newType) + fileName;

	        storage.copy(
	            Storage.CopyRequest.of(
	                bucketName,
	                oldPath,
	                BlobId.of(bucketName, newPath)
	            )
	        );

	        storage.delete(bucketName, oldPath);
	        return newPath;
	    } 
	    
	    
	// 삭제 이벤트 리스너용은 FileDeleteEventListener 에 있음
	    
}