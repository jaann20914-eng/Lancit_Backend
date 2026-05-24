package com.ssafy.lancit.domain.file.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.global.enums.FileParentType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
 
    private final FileService fileService;
 
    /** 파일 업로드 (GCS 업로드 + DB 저장) */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileDTO>> upload(
            @RequestParam MultipartFile file,
            @RequestParam FileParentType parentType,
            @RequestParam(required = false) Integer parentId) {
        // TODO 지원: fileService.upload(file, parentType, parentId, email, ownerType)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** 파일 삭제 (DB 삭제 + GCS 삭제 이벤트) */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable int fileId) {
        // TODO 지원: fileService.delete(fileId)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
 