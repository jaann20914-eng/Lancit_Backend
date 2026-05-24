package com.ssafy.lancit.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * USER-03 마이페이지 조회
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 이메일 꺼내기
     * TODO 지원 [2]: userService.getMe(email) 호출
     * TODO 지원 [3]: ApiResponse.ok(userDTO) 반환
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getMe() {
        // TODO 지원 [1] ~ [3] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * USER-04 마이페이지 수정
     * - 비밀번호 변경 시 BCrypt 암호화는 서비스에서 처리
     * - 프로필 사진 변경 시
     *   1. /api/files/upload 로 새 파일 업로드 → fileId 반환
     *   2. 해당 fileId 를 dto.profileFileId 에 담아서 이 API 호출
     *   3. 기존 파일 삭제는 /api/files/{fileId} DELETE 로 별도 호출
     *
     * TODO 지원 [1]: SecurityUtil.getCurrentEmail() 로 이메일 꺼내기
     * TODO 지원 [2]: dto.setEmail(email) 세팅
     * TODO 지원 [3]: userService.update(dto) 호출
     * TODO 지원 [4]: ApiResponse.ok(null) 반환
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMe(@RequestBody UserDTO dto) {
        // TODO 지원 [1] ~ [4] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * USER-02 회원 탈퇴
     * - @ContractGuard → ContractGuardAspect 에서 진행 중 계약 있으면 차단
     * - Category(ownerType=USER), Task(ownerType=USER) 앱 레벨 삭제
     * - User 삭제 → File, Portfolio, RecruitmentApplication,
     *   Bookmark, ChatRoom, Message, Proposal CASCADE 자동 삭제
     *
     * TODO 지원 [1]: userService.delete() 호출
     *               - 이메일은 서비스 내부에서 SecurityUtil.getCurrentEmail() 로 꺼냄
     * TODO 지원 [2]: ApiResponse.ok(null) 반환
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        // TODO 지원 [1] ~ [2] 구현
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}