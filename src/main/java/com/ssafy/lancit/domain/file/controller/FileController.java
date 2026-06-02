package com.ssafy.lancit.domain.file.controller;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.global.enums.FileParentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// 파일 업로드 / Signed URL 조회 / 삭제
// ★ Redis: Signed URL 캐싱 (FileService @Cacheable, @CacheEvict 에서 처리)
// ★ GCS: 업로드/삭제 (GcsService 에서 처리, 삭제는 FileDeleteEvent 커밋 후 실행)
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    // 파일 업로드 - GCS 업로드 + DB 저장
    // 프로필 사진: parentType=PROFILE, parentId=null
    // 포트폴리오 배너/결과물: parentType=PORTFOLIO, parentId=portfolioId
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileDTO>> upload(
            @RequestParam MultipartFile file,
            @RequestParam FileParentType parentType,
            @RequestParam(required = false) Integer parentId) {
        // TODO 지원 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: String role = SecurityUtil.getCurrentRole()
        // TODO 지원 [3]: FileDTO dto = fileService.upload(file, parentType, parentId, email, role)
        // TODO 지원 [4]: return ResponseEntity.ok(ApiResponse.ok(dto))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // Signed URL 조회
    // ★ Redis 캐시 조회 → 없으면 GCS 발급 후 6일 TTL 저장
    // 프론트에서 이미지/파일 렌더링 시 이 API 호출
    @GetMapping("/{fileId}/url")
    public ResponseEntity<ApiResponse<String>> getSignedUrl(@PathVariable int fileId) {
        // TODO 지원 [1]: String url = fileService.getSignedUrl(fileId)
        // TODO 지원 [2]: return ResponseEntity.ok(ApiResponse.ok(url))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 파일 삭제 - 본인 파일만 삭제 가능
    // ★ fileService.delete() → DB 삭제 + Redis @CacheEvict + FileDeleteEvent 발행
    // ★ FileDeleteEvent → 트랜잭션 커밋 후 GCS 실제 파일 삭제
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable int fileId) {
        // TODO 지원 [1]: FileDTO dto = fileService.findById(fileId)
        // TODO 지원 [2]: 본인 파일 검증
        //               String email = SecurityUtil.getCurrentEmail()
        //               dto.getUserEmail() 또는 dto.getCompanyEmail() 이 email 과 다르면
        //               throw new CustomException(ErrorCode.FORBIDDEN)
        // TODO 지원 [3]: fileService.delete(fileId)
        // TODO 지원 [4]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}