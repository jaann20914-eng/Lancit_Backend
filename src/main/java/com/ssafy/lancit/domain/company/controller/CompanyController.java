package com.ssafy.lancit.domain.company.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.service.CompanyService;

import lombok.RequiredArgsConstructor;

// 회사 마이페이지 (조회 / 수정 / 탈퇴)
// 프로필 사진은 FileController 에서 별도 처리 (프론트가 /api/files/{id}/url 별도 호출)
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // 응답에 profileFileId 포함 → 프론트가 .then () 으로 파일아이디 가지고 signed URL 로 바로 받아오기
    // TODO 지원: 이후 시큐리티에서 role에 따라서 접근가능한 패키지명 제어 필요
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CompanyDTO>> getMe() {
    	String email = SecurityUtil.getCurrentEmail();
    	CompanyDTO company = companyService.getMe(email);
        return ResponseEntity.ok(ApiResponse.ok(company));
    }

    // CLI-USER-04 마이페이지 수정
    //1. 사진 선택
    //POST /api/files/upload (parentType=TEMP, parentId=null)
    //→ fileId 반환
    // 2. 미리보기
    //GET /api/files/{fileId}/url → 화면에 표시
    // 3. 저장 버튼
    // PUT /api/user/me { profileFileId: fileId, ... }
    //→ UserService.update() 에서
    //① 기존 profileFileId 있으면 file_delete_queue 에 추가 (GCS 삭제 예약)
    //② file 테이블 parent_type TEMP → PROFILE 업데이트
    //③ user 테이블 profile_file_id 업데이트
    // 4. 취소 시
    //→ TempFileCleanupScheduler 가 24시간 후 정리
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMe(@RequestBody CompanyDTO dto) {
        String email = SecurityUtil.getCurrentEmail();
        dto.setEmail(email);
        companyService.update(dto);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-USER-02 회원 탈퇴
    // - @ContractGuard → 진행 중 계약 있으면 차단
    // 삭제 순서 (CompanyService 에서 처리):
    //   1. GCS + Redis 먼저 정리 (파일 목록 조회 → FileDeleteEvent 발행)
    //   2. Task, Category 앱 레벨 삭제
    //   3. companyMapper.delete() → DB CASCADE 로 나머지 자동 삭제
    //      (file, recruitment → application, bookmark, chatroom, message, proposal)
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
    	companyService.delete();
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}