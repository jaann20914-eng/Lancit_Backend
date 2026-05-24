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

    /**
     * 파일 업로드 (GCS 업로드 + DB 저장)
     * 사용처: 프로필 사진(PROFILE), 포트폴리오 파일은 PortfolioService 내부 호출
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 이메일 꺼내기
     * TODO 지원 [2]: SecurityUtil.getCurrentRole() 로 role 꺼내기
     *               - "USER"    → userEmail 세팅
     *               - "COMPANY" → companyEmail 세팅
     * TODO 지원 [3]: fileService.upload(file, parentType, parentId, email, role) 호출
     * TODO 지원 [4]: 반환된 FileDTO 를 ApiResponse.ok() 에 담아 반환
     *               - 프로필 사진의 경우 반환된 fileId 를
     *                 프론트에서 받아 PUT /api/user/me 로 profileFileId 업데이트
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileDTO>> upload(
            @RequestParam MultipartFile file,
            @RequestParam FileParentType parentType,
            @RequestParam(required = false) Integer parentId) {

        // TODO 지원 [1] ~ [4] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 파일 삭제 (DB 삭제 + GCS 삭제 이벤트)
     * 사용처: 프로필 사진 변경 시 기존 파일 삭제
     *
     * TODO 지원 [1]: fileService.findById(fileId) 로 파일 조회
     *               - 없으면 throw new CustomException(ErrorCode.FILE_NOT_FOUND)
     * TODO 지원 [2]: 본인 파일인지 검증
     *               - file.getUserEmail() 또는 file.getCompanyEmail() 이
     *                 SecurityUtil.getCurrentEmail() 과 다르면
     *                 throw new CustomException(ErrorCode.FORBIDDEN)
     * TODO 지원 [3]: fileService.delete(fileId) 호출
     *               - DB 삭제 + FileDeleteEvent 발행
     *               - 트랜잭션 커밋 후 GcsService 가 GCS 자동 삭제
     * TODO 지원 [4]: ApiResponse.ok(null) 반환
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable int fileId) {

        // TODO 지원 [1] ~ [4] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}