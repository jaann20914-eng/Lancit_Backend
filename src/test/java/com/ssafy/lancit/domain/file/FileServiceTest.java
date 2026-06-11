package com.ssafy.lancit.domain.file;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.global.enums.FileParentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileServiceTest {

    @Autowired FileService fileService;
    @Autowired FileMapper fileMapper;
    @Autowired CacheManager cacheManager;
    @Autowired Storage storage; // GCS 실제 확인용

    @Value("${gcs.bucket-name}")
    String bucketName;

    static int userFileId;
    static int companyFileId;
    static int multiFileId1;
    static int multiFileId2;
    static String userUploadPath;    // GCS 경로 확인용
    static String multiUploadPath1;
    static String multiUploadPath2;

    void setAuth(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // GCS 에 실제로 파일이 존재하는지 확인하는 헬퍼
    boolean existsInGcs(String uploadPath) {
        Blob blob = storage.get(BlobId.of(bucketName, uploadPath));
        return blob != null && blob.exists();
    }

    // ── 업로드 + GCS 확인 ────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("유저 단건 업로드 - PROFILE + GCS 실제 저장 확인")
    void uploadUserProfile() throws Exception {
        setAuth("test@lancit.com", "user");
        MockMultipartFile file = new MockMultipartFile(
                "files", "user-profile.jpg", "image/jpeg", "user-image".getBytes());

        List<FileDTO> result = fileService.upload(
                List.of(file), FileParentType.PROFILE, null, "test@lancit.com", "user");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserEmail()).isEqualTo("test@lancit.com");
        assertThat(result.get(0).getCompanyEmail()).isNull();
        assertThat(result.get(0).getUploadPath()).startsWith("profile/");

        userFileId = result.get(0).getFileId();
        userUploadPath = result.get(0).getUploadPath();

        // ★ GCS 실제 저장 확인
        assertThat(existsInGcs(userUploadPath)).isTrue();
        System.out.println("[유저 업로드] fileId: " + userFileId + " GCS 저장 확인 ✅");
    }

    @Test
    @Order(2)
    @DisplayName("회사 단건 업로드 - PROFILE + GCS 실제 저장 확인")
    void uploadCompanyProfile() throws Exception {
        setAuth("company@lancit.com", "company");
        MockMultipartFile file = new MockMultipartFile(
                "files", "company-profile.jpg", "image/jpeg", "company-image".getBytes());

        List<FileDTO> result = fileService.upload(
                List.of(file), FileParentType.PROFILE, null, "company@lancit.com", "company");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyEmail()).isEqualTo("company@lancit.com");
        assertThat(result.get(0).getUserEmail()).isNull();

        companyFileId = result.get(0).getFileId();

        // ★ GCS 실제 저장 확인
        assertThat(existsInGcs(result.get(0).getUploadPath())).isTrue();
        System.out.println("[회사 업로드] fileId: " + companyFileId + " GCS 저장 확인 ✅");
    }

    @Test
    @Order(3)
    @DisplayName("유저 다건 업로드 - PORTFOLIO_FILE + GCS 실제 저장 확인")
    void uploadMultipleFiles() throws Exception {
        setAuth("test@lancit.com", "user");
        MockMultipartFile f1 = new MockMultipartFile("files", "f1.jpg", "image/jpeg", "c1".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "f2.jpg", "image/jpeg", "c2".getBytes());

        List<FileDTO> result = fileService.upload(
                List.of(f1, f2), FileParentType.PORTFOLIO_FILE, 1, "test@lancit.com", "user");

        assertThat(result).hasSize(2);
        result.forEach(dto -> assertThat(dto.getUploadPath()).startsWith("portfolio/file/"));

        multiFileId1 = result.get(0).getFileId();
        multiFileId2 = result.get(1).getFileId();
        multiUploadPath1 = result.get(0).getUploadPath();
        multiUploadPath2 = result.get(1).getUploadPath();

        // ★ GCS 실제 저장 확인 (다건 전부)
        assertThat(existsInGcs(multiUploadPath1)).isTrue();
        assertThat(existsInGcs(multiUploadPath2)).isTrue();
        System.out.println("[다건 업로드] " + result.size() + "개 GCS 저장 확인 ✅");
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
        fileService.getSignedUrl(userFileId);
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

    // ── 다운로드 URL 조회 ────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("단건 다운로드 URL 조회 - attachment 포함 확인")
    void getDownloadUrlSingle() {
        String url = fileService.getDownloadUrl(multiFileId1);
        assertThat(url).isNotNull().contains("storage.googleapis.com");
        // Redis 캐싱 안 함 확인 → 캐시에 없어야 함
        var cached = cacheManager.getCache("signedUrl").get(multiFileId1);
        assertThat(cached).isNull();
        System.out.println("[단건 다운로드 URL] " + url);
    }

    @Test
    @Order(8)
    @DisplayName("다건 다운로드 URL 조회 - 각 파일마다 URL 발급 확인")
    void getDownloadUrlMultiple() {
        String url1 = fileService.getDownloadUrl(multiFileId1);
        String url2 = fileService.getDownloadUrl(multiFileId2);

        assertThat(url1).isNotNull().contains("storage.googleapis.com");
        assertThat(url2).isNotNull().contains("storage.googleapis.com");
        assertThat(url1).isNotEqualTo(url2); // 파일 다르면 URL 달라야 함
        System.out.println("[다건 다운로드 URL1] " + url1);
        System.out.println("[다건 다운로드 URL2] " + url2);
    }

    @Test
    @Order(9)
    @DisplayName("다운로드 URL 조회 - 없는 fileId → FILE_NOT_FOUND")
    void getDownloadUrlNotFound() {
        assertThatThrownBy(() -> fileService.getDownloadUrl(99999))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("파일을 찾을 수 없습니다");
    }

    // ── OwnerCheck 검증 ──────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("OwnerCheck - 본인 파일 삭제 성공 + GCS 삭제 확인")
    void deleteOwnerSuccess() {
        setAuth("test@lancit.com", "user");
        assertThatNoException().isThrownBy(() -> fileService.delete(userFileId));

        assertThat(fileMapper.findById(userFileId)).isNull();
        assertThat(cacheManager.getCache("signedUrl").get(userFileId)).isNull();

        // ★ GCS 실제 삭제 확인
        assertThat(existsInGcs(userUploadPath)).isFalse();
        System.out.println("[OwnerCheck] 본인 파일 삭제 성공 + GCS 삭제 확인 ✅");
    }

    @Test
    @Order(11)
    @DisplayName("OwnerCheck - 유저가 회사 파일 삭제 시도 → FORBIDDEN")
    void deleteOwnerFail_UserTriesCompanyFile() {
        setAuth("test@lancit.com", "user");
        assertThatThrownBy(() -> fileService.delete(companyFileId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("접근 권한이 없습니다");
        System.out.println("[OwnerCheck] 유저 → 회사 파일 접근 차단 확인");
    }

    @Test
    @Order(12)
    @DisplayName("OwnerCheck - 다른 유저 파일 삭제 시도 → FORBIDDEN")
    void deleteOwnerFail_OtherUser() throws Exception {
        setAuth("test２@lancit.com", "user");
        MockMultipartFile file = new MockMultipartFile(
                "files", "other.jpg", "image/jpeg", "other".getBytes());
        List<FileDTO> uploaded = fileService.upload(
                List.of(file), FileParentType.PROFILE, null, "test２@lancit.com", "user");
        int otherFileId = uploaded.get(0).getFileId();

        setAuth("test@lancit.com", "user");
        assertThatThrownBy(() -> fileService.delete(otherFileId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("접근 권한이 없습니다");
        System.out.println("[OwnerCheck] 다른 유저 파일 접근 차단 확인");

        setAuth("test２@lancit.com", "user");
        fileService.delete(otherFileId);
    }

    @Test
    @Order(13)
    @DisplayName("OwnerCheck - 없는 fileId 삭제 → NOT_FOUND")
    void deleteNotFound() {
        setAuth("test@lancit.com", "user");
        assertThatThrownBy(() -> fileService.delete(99999))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("리소스를 찾을 수 없습니다");
        System.out.println("[OwnerCheck] 없는 파일 삭제 시도 → NOT_FOUND 확인");
    }

    // ── 부모 기준 전체 삭제 + GCS 확인 ─────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("deleteByParent - PORTFOLIO_FILE parentId=1 전체 삭제 + GCS 삭제 확인")
    void deleteByParent() {
        fileService.deleteByParent(FileParentType.PORTFOLIO_FILE, 1);

        List<FileDTO> remaining = fileService.findByParent(FileParentType.PORTFOLIO_FILE, 1);
        assertThat(remaining).isEmpty();

        // ★ GCS 실제 삭제 확인 (다건 전부)
        assertThat(existsInGcs(multiUploadPath1)).isFalse();
        assertThat(existsInGcs(multiUploadPath2)).isFalse();
        System.out.println("[부모 삭제] PORTFOLIO_FILE parentId=1 전체 삭제 + GCS 삭제 확인 ✅");
    }

    @Test
    @Order(15)
    @DisplayName("deleteByParent - 파일 없는 parentId → 정상 처리")
    void deleteByParentEmpty() {
        assertThatNoException().isThrownBy(
                () -> fileService.deleteByParent(FileParentType.PORTFOLIO_FILE, 99999));
        System.out.println("[부모 삭제] 빈 parentId 정상 처리 확인");
    }

    // ── 회사 파일 정리 ───────────────────────────────────────────────────────

    @Test
    @Order(16)
    @DisplayName("회사 파일 삭제 - 본인 확인 후 삭제 + GCS 삭제 확인")
    void deleteCompanyFile() {
        setAuth("company@lancit.com", "company");
        String companyUploadPath = fileMapper.findById(companyFileId).getUploadPath();

        assertThatNoException().isThrownBy(() -> fileService.delete(companyFileId));
        assertThat(fileMapper.findById(companyFileId)).isNull();

        // ★ GCS 실제 삭제 확인
        assertThat(existsInGcs(companyUploadPath)).isFalse();
        System.out.println("[회사 파일 삭제] 완료 + GCS 삭제 확인 ✅");
    }
}
