package com.ssafy.lancit.domain.user.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 프리랜서 마이페이지 (조회 / 수정 / 탈퇴)
// 프로필 사진 이미지는 GET /api/files/{profileFileId}/url 로 별도 호출
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // USER-03 마이페이지 조회
    // 응답에 profileFileId 포함 → 프론트가 .then ( GET /api/files/{profileFileId}/url ) 로 Signed URL 별도 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getMe() {
        // TODO 지원 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: UserDTO dto = userService.getMe(email)
        // TODO 지원 [3]: return ResponseEntity.ok(ApiResponse.ok(dto))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // USER-04 마이페이지 수정
    // 프로필 사진 변경 흐름:
    //   1. POST /api/files/upload → fileId 반환
    //   2. dto.profileFileId 에 담아서 이 API 호출 .then (PUT /api/user/me)로 오게 될거임
    //   3. 기존 사진 삭제는 DELETE /api/files/{fileId} 별도 호출
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMe(@RequestBody UserDTO dto) {
        // TODO 지원 [1]: String email = SecurityUtil.getCurrentEmail()
        // TODO 지원 [2]: dto.setEmail(email)
        // TODO 지원 [3]: userService.update(dto)
        // TODO 지원 [4]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // USER-02 회원 탈퇴
    // - @ContractGuard → 진행 중 계약 있으면 차단
    // - Category, Task 앱 레벨 삭제 후 User 삭제
    // - User 삭제 시 CASCADE: File, Portfolio, RecruitmentApplication,
    //                         Bookmark, ChatRoom, Message, Proposal 자동 삭제
    // 파일 삭제 이벤트(FileDeleteEvent) → 트랜잭션 커밋 후 GCS 파일도 자동 삭제
    // Redis Signed URL 캐시도 @CacheEvict 로 자동 제거
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        // TODO 지원 [1]: userService.delete() 호출 (이메일은 서비스 내부에서 SecurityUtil 로 꺼냄)
        // TODO 지원 [2]: return ResponseEntity.ok(ApiResponse.ok(null))
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}