package com.ssafy.lancit.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;
 
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
 
    private final UserService userService;
 
    /** USER-03 마이페이지 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getMe() {
        // TODO 지원: SecurityUtil.getCurrentEmail() → userService.getMe(email)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** USER-04 마이페이지 수정 */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMe(@RequestBody UserDTO dto) {
        // TODO 지원: userService.update(dto) 호출 (프로필 이미지 변경 포함)
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
 
    /** USER-02 회원 탈퇴 (@ContractGuard → 진행 중 계약 있으면 차단) */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        // TODO 지원: userService.delete() 호출
        //   → Category(ownerType=USER), Task(ownerType=USER) 앱 레벨 삭제 후 User 삭제
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}