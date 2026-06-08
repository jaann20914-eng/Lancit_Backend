package com.ssafy.lancit.domain.user.controller;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
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

    // 마이페이지 조회
    // 응답에 profileFileId 포함 → 프론트가 .then () 으로 파일아이디 가지고 signed URL 로 바로 받아오기
    // TODO 지원: 이후 시큐리티에서 role에 따라서 접근가능한 패키지명 제어 필요
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getMe() {
    	String email = SecurityUtil.getCurrentEmail();    	
    	UserDTO dto = userService.getMe(email);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    //마이페이지 수정
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
    public ResponseEntity<ApiResponse<Void>> updateMe(@RequestBody UserDTO dto) {
    	String email = SecurityUtil.getCurrentEmail();
        dto.setEmail(email);
        userService.update(dto);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    //회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMe() {
        userService.delete();
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}