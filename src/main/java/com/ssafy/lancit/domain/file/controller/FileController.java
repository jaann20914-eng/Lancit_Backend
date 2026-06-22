package com.ssafy.lancit.domain.file.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.file.service.GcsService;
import com.ssafy.lancit.global.enums.FileParentType;

import lombok.RequiredArgsConstructor;

// 파일 업로드 / Signed URL 조회 / 삭제
// ★ Redis: Signed URL 캐싱 (FileService @Cacheable, @CacheEvict 에서 처리)
// ★ GCS: 업로드/삭제 (GcsService 에서 처리, 삭제는 FileDeleteEvent 커밋 후 실행)
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final GcsService gcsService;

    // 파일 업로드 - GCS 업로드 + DB 저장
    // 프로필 사진: parentType=PROFILE, parentId=null
    // 포트폴리오 배너/결과물: parentType=PORTFOLIO_BANNER 또는 PORTFOLIO_FILE, parentId=portfolioId
    //    @PostMapping("/upload")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<FileDTO>> > upload(
    		@RequestPart List<MultipartFile> files,
            @RequestParam FileParentType parentType,
            @RequestParam(required = false) Integer parentId) {
    	String email = SecurityUtil.getCurrentEmail();
        String role  = SecurityUtil.getCurrentRole();
        List<FileDTO> result = fileService.upload(files, parentType, parentId, email, role);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    
    
    
    // Signed URL 조회
    // Redis 캐시 조회 → 없으면 GCS 발급 후 6일 TTL 저장
    // 프론트에서 이미지/파일 렌더링 시 이 API 호출
    @GetMapping("/{fileId}/url")
    public ResponseEntity<ApiResponse<String>> getSignedUrl(@PathVariable int fileId) {
        String email = SecurityUtil.getCurrentEmail();
        fileService.validateReadAccess(fileId, email);
        String url = fileService.getSignedUrl(fileId);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    
    
    
    // 파일 삭제 - 본인 파일만 삭제 가능
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable int fileId) {
    	fileService.delete(fileId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
   
    
    
    // 파일 다운로드 - Signed URL 반환 (프론트가 직접 GCS 에서 다운로드)
//    @GetMapping("/{fileId}/download")
//    public ResponseEntity<ApiResponse<String>> download(@PathVariable int fileId) {
//        String email = SecurityUtil.getCurrentEmail();
//        fileService.validateReadAccess(fileId, email);
//        String url = fileService.getDownloadUrl(fileId);
//        return ResponseEntity.ok(ApiResponse.ok(url));
//    }
    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> download(@PathVariable int fileId) {
        String email = SecurityUtil.getCurrentEmail();
        fileService.validateReadAccess(fileId, email);

        FileDTO dto = fileService.findById(fileId);
        byte[] bytes = gcsService.download(dto.getUploadPath());

        String encodedName;
        try {
            encodedName = java.net.URLEncoder.encode(dto.getOriName(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            encodedName = "file";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
    
    
}
