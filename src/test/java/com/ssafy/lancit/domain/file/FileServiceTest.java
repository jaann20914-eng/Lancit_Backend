package com.ssafy.lancit.domain.file;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.global.enums.FileParentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
//1~5업로드 (유저/회사/다건/빈 리스트)
//6~8Signed URL (GCS발급/Redis캐시/NOT_FOUND)
//9~14OwnerCheck (성공/실패 5가지)
//15~17deleteByParent (삭제/타입혼용/빈parentId)
//18회사 파일 정리


@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileServiceTest {
 
    @Autowired FileService fileService;
    @Autowired FileMapper fileMapper;
    @Autowired CacheManager cacheManager;
 
    // 테스트 간 공유할 fileId
    static int userFileId;    // test@lancit.com 이 올린 파일
    static int companyFileId; // company@lancit.com 이 올린 파일
 
    // 인증 세팅 헬퍼
    void setAuth(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
 
    // ── 업로드 ──────────────────────────────────────────────────────────────
 
    @Test
    @Order(1)
    @DisplayName("유저 단건 업로드 - PROFILE")
    void uploadUserProfile() throws Exception {
        setAuth("test@lancit.com", "USER");
 
        MockMultipartFile file = new MockMultipartFile(
                "files", "user-profile.jpg", "image/jpeg", "user-image".getBytes());
 
        List<FileDTO> result = fileService.upload(
                List.of(file), FileParentType.PROFILE, null, "test@lancit.com", "USER");
 
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserEmail()).isEqualTo("test@lancit.com");
        assertThat(result.get(0).getCompanyEmail()).isNull();
        assertThat(result.get(0).getUploadPath()).startsWith("profile/");
 
        userFileId = result.get(0).getFileId();
        System.out.println("[유저 업로드] fileId: " + userFileId);
    }
 
    @Test
    @Order(2)
    @DisplayName("회사 단건 업로드 - PROFILE")
    void uploadCompanyProfile() throws Exception {
        setAuth("company@lancit.com", "COMPANY");
 
        MockMultipartFile file = new MockMultipartFile(
                "files", "company-profile.jpg", "image/jpeg", "company-image".getBytes());
 
        List<FileDTO> result = fileService.upload(
                List.of(file), FileParentType.PROFILE, null, "company@lancit.com", "COMPANY");
 
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyEmail()).isEqualTo("company@lancit.com");
        assertThat(result.get(0).getUserEmail()).isNull();
 
        companyFileId = result.get(0).getFileId();
        System.out.println("[회사 업로드] fileId: " + companyFileId);
    }
 
    @Test
    @Order(3)
    @DisplayName("유저 다건 업로드 - PORTFOLIO_FILE")
    void uploadMultipleFiles() throws Exception {
        setAuth("test@lancit.com", "USER");
 
        MockMultipartFile f1 = new MockMultipartFile("files", "f1.jpg", "image/jpeg", "c1".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "f2.jpg", "image/jpeg", "c2".getBytes());
 
        List<FileDTO> result = fileService.upload(
                List.of(f1, f2), FileParentType.PORTFOLIO_FILE, 1, "test@lancit.com", "USER");
 
        assertThat(result).hasSize(2);
        result.forEach(dto -> assertThat(dto.getUploadPath()).startsWith("portfolio/file/"));
        System.out.println("[다건 업로드] " + result.size() + "개 완료");
    }
 
    // ── Signed URL 조회 ──────────────────────────────────────────────────────
 
    @Test
    @Order(4)
    @DisplayName("Signed URL 최초 조회 - GCS 에서 발급")
    void getSignedUrlFirst() {
        String url = fileService.getSignedUrl(userFileId);
        assertThat(url).isNotNull().contains("storage.googleapis.com");
        System.out.println("[Signed URL] " + url);
    }
 
    @Test
    @Order(5)
    @DisplayName("Signed URL 재조회 - Redis 캐시에서 반환")
    void getSignedUrlCached() {
        fileService.getSignedUrl(userFileId); // 캐싱
        var cached = cacheManager.getCache("signedUrl").get(userFileId);
        assertThat(cached).isNotNull();
        System.out.println("[Redis 캐시] " + cached.get());
    }
 
    @Test
    @Order(6)
    @DisplayName("Signed URL 조회 - 없는 fileId → FILE_NOT_FOUND")
    void getSignedUrlNotFound() {
        assertThatThrownBy(() -> fileService.getSignedUrl(99999))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("파일을 찾을 수 없습니다");
    }
 
    // ── OwnerCheck 검증 ──────────────────────────────────────────────────────
 
    @Test
    @Order(7)
    @DisplayName("OwnerCheck - 본인 파일 삭제 성공 (유저)")
    void deleteOwnerSuccess() {
        setAuth("test@lancit.com", "USER");
        assertThatNoException().isThrownBy(() -> fileService.delete(userFileId));
 
        // DB 삭제 확인
        assertThat(fileMapper.findById(userFileId)).isNull();
        // Redis 캐시 제거 확인
        assertThat(cacheManager.getCache("signedUrl").get(userFileId)).isNull();
        System.out.println("[OwnerCheck] 본인 파일 삭제 성공");
    }
 
    @Test
    @Order(8)
    @DisplayName("OwnerCheck - 타인 파일 삭제 시도 → FORBIDDEN (유저가 회사 파일 삭제 시도)")
    void deleteOwnerFail_UserTriesCompanyFile() {
        setAuth("test@lancit.com", "USER"); // 유저로 로그인
        assertThatThrownBy(() -> fileService.delete(companyFileId)) // 회사 파일 삭제 시도
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("접근 권한이 없습니다");
        System.out.println("[OwnerCheck] 타인 파일 접근 차단 확인");
    }
 
    @Test
    @Order(9)
    @DisplayName("OwnerCheck - 다른 유저 파일 삭제 시도 → FORBIDDEN")
    void deleteOwnerFail_OtherUser() throws Exception {
        // test２@lancit.com 이 파일 업로드
        setAuth("test２@lancit.com", "USER");
        MockMultipartFile file = new MockMultipartFile(
                "files", "other.jpg", "image/jpeg", "other".getBytes());
        List<FileDTO> uploaded = fileService.upload(
                List.of(file), FileParentType.PROFILE, null, "test２@lancit.com", "USER");
        int otherFileId = uploaded.get(0).getFileId();
 
        // test@lancit.com 이 삭제 시도
        setAuth("test@lancit.com", "USER");
        assertThatThrownBy(() -> fileService.delete(otherFileId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("접근 권한이 없습니다");
        System.out.println("[OwnerCheck] 다른 유저 파일 접근 차단 확인");
 
        // 정리
        setAuth("test２@lancit.com", "USER");
        fileService.delete(otherFileId);
    }
 
    @Test
    @Order(10)
    @DisplayName("OwnerCheck - 없는 fileId 삭제 → NOT_FOUND")
    void deleteNotFound() {
        setAuth("test@lancit.com", "USER");
        assertThatThrownBy(() -> fileService.delete(99999))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("리소스를 찾을 수 없습니다");
        System.out.println("[OwnerCheck] 없는 파일 삭제 시도 → NOT_FOUND 확인");
    }
 
    // ── 부모 기준 전체 삭제 ──────────────────────────────────────────────────
 
    @Test
    @Order(11)
    @DisplayName("부모 기준 전체 삭제 - PORTFOLIO_FILE parentId=1")
    void deleteByParent() {
        setAuth("test@lancit.com", "USER");
        fileService.deleteByParent(FileParentType.PORTFOLIO_FILE, 1);
 
        List<FileDTO> remaining = fileService.findByParent(FileParentType.PORTFOLIO_FILE, 1);
        assertThat(remaining).isEmpty();
        System.out.println("[부모 삭제] PORTFOLIO_FILE parentId=1 전체 삭제 완료");
    }
 
    // ── 회사 파일 정리 ───────────────────────────────────────────────────────
 
    @Test
    @Order(12)
    @DisplayName("회사 파일 삭제 - 본인 확인 후 삭제")
    void deleteCompanyFile() {
        setAuth("company@lancit.com", "COMPANY");
        assertThatNoException().isThrownBy(() -> fileService.delete(companyFileId));
        assertThat(fileMapper.findById(companyFileId)).isNull();
        System.out.println("[회사 파일 삭제] 완료");
    }
}