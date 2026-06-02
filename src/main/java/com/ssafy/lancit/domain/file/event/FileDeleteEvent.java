package com.ssafy.lancit.domain.file.event;

/**
 * GCS 파일 삭제 이벤트
 * - 트랜잭션 커밋 이후 GcsService.delete() 를 실행하기 위해 사용
 * - @TransactionalEventListener(phase = AFTER_COMMIT) 로 처리
 */


//트랜잭션 커밋 후 GCS 파일 삭제 트리거 이벤트 - GcsService.handleFileDelete() 가 수신
public class FileDeleteEvent {
    private final String uploadPath;
 
    public FileDeleteEvent(String uploadPath) {
        this.uploadPath = uploadPath;
    }
 
    public String getUploadPath() {
        return uploadPath;
    }
}
