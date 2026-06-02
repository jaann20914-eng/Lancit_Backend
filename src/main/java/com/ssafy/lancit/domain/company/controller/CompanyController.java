package com.ssafy.lancit.domain.company.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 회사 마이페이지 (조회 / 수정 / 탈퇴)
// 프로필 사진은 FileController 에서 별도 처리 (프론트가 /api/files/{id}/url 별도 호출)
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    // CLI-USER-03 마이페이지 조회 - profileFileId 포함 반환 (Signed URL 은 프론트가 별도 호출)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CompanyDTO>> getMe() {
        // TODO 지원 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: CompanyDTO dto = companyService.getMe(email)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(dto))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // CLI-USER-04 마이페이지 수정
    // 프로필 사진 변경 흐름:
    //   1. POST /api/files/upload → fileId 반환
    //   2. dto.profileFileId 에 담아서 이 API 호출
    //   3. 기존 사진 삭제는 DELETE /api/files/{fileId} 별도 호출
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMe(@RequestBody CompanyDTO dto) {
        // TODO 지원 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: dto.setEmail(email)
        // TODO 지원 [3]: companyService.update(dto)
        // TODO 지원 [4]: return ResponseEntity.ok(ApiResponse.ok(null))
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
        // TODO 지원 [1]: companyService.delete()
        // TODO 지원 [2]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}