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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FileServiceTest {

    private static final String RUN_ID = UUID.randomUUID().toString();
    private static final String USER_EMAIL = "file-test-user-" + RUN_ID + "@lancit.test";
    private static final String OTHER_USER_EMAIL = "file-test-other-" + RUN_ID + "@lancit.test";
    private static final String COMPANY_EMAIL = "file-test-company-" + RUN_ID + "@lancit.test";
    private static final int PARENT_ID = Math.floorMod(RUN_ID.hashCode(), 1_000_000_000) + 1;
    private static final int NOT_FOUND_FILE_ID = -1;

    @Autowired FileService fileService;
    @Autowired FileMapper fileMapper;
    @Autowired CacheManager cacheManager;
    @Autowired Storage storage; // GCS 실제 확인용
    @Autowired JdbcTemplate jdbcTemplate;

    @Value("${gcs.bucket-name}")
    String bucketName;

    private final Set<Integer> uploadedFileIds = new HashSet<>();
    private final Set<String> uploadedPaths = new HashSet<>();

    @BeforeAll
    void createOwnerFixtures() {
        jdbcTemplate.update("""
                INSERT INTO `user` (email, password, name, phone, job_category)
                VALUES (?, ?, ?, ?, 'IT')
                """, USER_EMAIL, "test-password", "파일 테스트 유저", "01000000001");
        jdbcTemplate.update("""
                INSERT INTO `user` (email, password, name, phone, job_category)
                VALUES (?, ?, ?, ?, 'IT')
                """, OTHER_USER_EMAIL, "test-password", "파일 테스트 다른 유저", "01000000002");
        jdbcTemplate.update("""
                INSERT INTO company (email, password, name, company_name, phone, job_category)
                VALUES (?, ?, ?, ?, ?, 'IT')
                """, COMPANY_EMAIL, "test-password", "파일 테스트 담당자", "파일 테스트 회사", "01000000003");
    }

    @AfterEach
    void cleanUpAfterEach() {
        cleanUpOwnedFiles();
        uploadedPaths.forEach(path -> storage.delete(BlobId.of(bucketName, path)));
        var cache = cacheManager.getCache("signedUrl");
        if (cache != null) {
            uploadedFileIds.forEach(fileId -> {
                cache.evict(fileId);
                cache.evict(String.valueOf(fileId));
            });
        }
        uploadedFileIds.clear();
        uploadedPaths.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterAll
    void removeOwnerFixtures() {
        cleanUpOwnedFiles();
        jdbcTemplate.update("DELETE FROM company WHERE email = ?", COMPANY_EMAIL);
        jdbcTemplate.update("DELETE FROM `user` WHERE email IN (?, ?)", USER_EMAIL, OTHER_USER_EMAIL);
    }

    private void cleanUpOwnedFiles() {
        List<StoredFile> files = jdbcTemplate.query("""
                        SELECT file_id, upload_path
                        FROM file
                        WHERE user_email IN (?, ?) OR company_email = ?
                        """,
                (rs, rowNum) -> new StoredFile(rs.getInt("file_id"), rs.getString("upload_path")),
                USER_EMAIL, OTHER_USER_EMAIL, COMPANY_EMAIL);

        for (StoredFile file : files) {
            storage.delete(BlobId.of(bucketName, file.uploadPath()));
            jdbcTemplate.update("DELETE FROM file WHERE file_id = ?", file.fileId());
        }
    }

    private List<FileDTO> upload(
            String email,
            String role,
            FileParentType parentType,
            Integer parentId,
            MultipartFile... files
    ) throws Exception {
        setAuth(email, role);
        List<FileDTO> uploaded = fileService.upload(List.of(files), parentType, parentId, email, role);
        uploaded.forEach(file -> {
            uploadedFileIds.add(file.getFileId());
            uploadedPaths.add(file.getUploadPath());
        });
        return uploaded;
    }

    private record StoredFile(int fileId, String uploadPath) {}

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

    // ── 업로드 + GCS 확인 ───────────────────────────────────

    @Test
    @DisplayName("유저 단건 업로드 - PROFILE + GCS 실제 저장 확인")
    void uploadUserProfile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "user-profile.jpg", "image/jpeg", "user-image".getBytes());

        List<FileDTO> result = upload(USER_EMAIL, "USER", FileParentType.PROFILE, null, file);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(result.get(0).getCompanyEmail()).isNull();
        assertThat(result.get(0).getUploadPath()).startsWith("profile/");
        assertThat(existsInGcs(result.get(0).getUploadPath())).isTrue();
    }

    @Test
    @DisplayName("회사 단건 업로드 - PROFILE + GCS 실제 저장 확인")
    void uploadCompanyProfile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "company-profile.jpg", "image/jpeg", "company-image".getBytes());

        List<FileDTO> result = upload(COMPANY_EMAIL, "COMPANY", FileParentType.PROFILE, null, file);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyEmail()).isEqualTo(COMPANY_EMAIL);
        assertThat(result.get(0).getUserEmail()).isNull();
        assertThat(existsInGcs(result.get(0).getUploadPath())).isTrue();
    }

    @Test
    @DisplayName("유저 다건 업로드 - PORTFOLIO_FILE + GCS 실제 저장 확인")
    void uploadMultipleFiles() throws Exception {
        MockMultipartFile f1 = new MockMultipartFile("files", "f1.jpg", "image/jpeg", "c1".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "f2.jpg", "image/jpeg", "c2".getBytes());

        List<FileDTO> result = upload(
                USER_EMAIL, "USER", FileParentType.PORTFOLIO_FILE, PARENT_ID, f1, f2);

        assertThat(result).hasSize(2);
        result.forEach(dto -> {
            assertThat(dto.getUploadPath()).startsWith("portfolio/file/");
            assertThat(existsInGcs(dto.getUploadPath())).isTrue();
        });
    }

    // ── Signed URL 조회 ──────────────────────────────────────

    @Test
    @DisplayName("Signed URL 최초 조회 - GCS 에서 발급")
    void getSignedUrlFirst() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "signed-url.jpg", "image/jpeg", "signed-url".getBytes());
        FileDTO uploaded = upload(USER_EMAIL, "USER", FileParentType.PROFILE, null, file).get(0);

        String url = fileService.getSignedUrl(uploaded.getFileId());

        assertThat(url).isNotNull().contains("storage.googleapis.com");
    }

    @Test
    @DisplayName("Signed URL 재조회 - Redis 캐시에서 반환")
    void getSignedUrlCached() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "cached-url.jpg", "image/jpeg", "cached-url".getBytes());
        FileDTO uploaded = upload(USER_EMAIL, "USER", FileParentType.PROFILE, null, file).get(0);

        String firstUrl = fileService.getSignedUrl(uploaded.getFileId());
        jdbcTemplate.update("DELETE FROM file WHERE file_id = ?", uploaded.getFileId());
        String cachedUrl = fileService.getSignedUrl(uploaded.getFileId());

        assertThat(cachedUrl).isEqualTo(firstUrl);
    }

    @Test
    @DisplayName("Signed URL 조회 - 없는 fileId → FILE_NOT_FOUND")
    void getSignedUrlNotFound() {
        assertThatThrownBy(() -> fileService.getSignedUrl(NOT_FOUND_FILE_ID))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("파일을 찾을 수 없습니다");
    }

    // ── 다운로드 URL 조회 ───────────────────────────────────

    @Test
    @DisplayName("단건 다운로드 URL 조회 - attachment 포함 확인")
    void getDownloadUrlSingle() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "download-single.jpg", "image/jpeg", "download-single".getBytes());
        FileDTO uploaded = upload(
                USER_EMAIL, "USER", FileParentType.PORTFOLIO_FILE, PARENT_ID, file).get(0);

        String url = fileService.getDownloadUrl(uploaded.getFileId());
        jdbcTemplate.update("DELETE FROM file WHERE file_id = ?", uploaded.getFileId());

        assertThat(url).isNotNull().contains("storage.googleapis.com");
        assertThatThrownBy(() -> fileService.getSignedUrl(uploaded.getFileId()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("파일을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("다건 다운로드 URL 조회 - 각 파일마다 URL 발급 확인")
    void getDownloadUrlMultiple() throws Exception {
        MockMultipartFile f1 = new MockMultipartFile("files", "download-1.jpg", "image/jpeg", "c1".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "download-2.jpg", "image/jpeg", "c2".getBytes());
        List<FileDTO> uploaded = upload(
                USER_EMAIL, "USER", FileParentType.PORTFOLIO_FILE, PARENT_ID, f1, f2);

        String url1 = fileService.getDownloadUrl(uploaded.get(0).getFileId());
        String url2 = fileService.getDownloadUrl(uploaded.get(1).getFileId());

        assertThat(url1).isNotNull().contains("storage.googleapis.com");
        assertThat(url2).isNotNull().contains("storage.googleapis.com");
        assertThat(url1).isNotEqualTo(url2);
    }

    @Test
    @DisplayName("다운로드 URL 조회 - 없는 fileId → FILE_NOT_FOUND")
    void getDownloadUrlNotFound() {
        assertThatThrownBy(() -> fileService.getDownloadUrl(NOT_FOUND_FILE_ID))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("파일을 찾을 수 없습니다");
    }

    // ── OwnerCheck 검증 ──────────────────────────────────────

    @Test
    @DisplayName("OwnerCheck - 본인 파일 삭제 성공 + GCS 삭제 확인")
    void deleteOwnerSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "delete-owner.jpg", "image/jpeg", "delete-owner".getBytes());
        FileDTO uploaded = upload(USER_EMAIL, "USER", FileParentType.PROFILE, null, file).get(0);
        fileService.getSignedUrl(uploaded.getFileId());

        assertThatNoException().isThrownBy(() -> fileService.delete(uploaded.getFileId()));

        assertThat(fileMapper.findById(uploaded.getFileId())).isNull();
        assertThatThrownBy(() -> fileService.getSignedUrl(uploaded.getFileId()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("파일을 찾을 수 없습니다");
        assertThat(existsInGcs(uploaded.getUploadPath())).isFalse();
    }

    @Test
    @DisplayName("OwnerCheck - 유저가 회사 파일 삭제 시도 → FORBIDDEN")
    void deleteOwnerFail_UserTriesCompanyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "company-owned.jpg", "image/jpeg", "company-owned".getBytes());
        FileDTO uploaded = upload(COMPANY_EMAIL, "COMPANY", FileParentType.PROFILE, null, file).get(0);

        setAuth(USER_EMAIL, "USER");
        assertThatThrownBy(() -> fileService.delete(uploaded.getFileId()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("접근 권한이 없습니다");
        assertThat(fileMapper.findById(uploaded.getFileId())).isNotNull();
        assertThat(existsInGcs(uploaded.getUploadPath())).isTrue();
    }

    @Test
    @DisplayName("OwnerCheck - 다른 유저 파일 삭제 시도 → FORBIDDEN")
    void deleteOwnerFail_OtherUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "other.jpg", "image/jpeg", "other".getBytes());
        FileDTO uploaded = upload(OTHER_USER_EMAIL, "USER", FileParentType.PROFILE, null, file).get(0);

        setAuth(USER_EMAIL, "USER");
        assertThatThrownBy(() -> fileService.delete(uploaded.getFileId()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("접근 권한이 없습니다");
        assertThat(fileMapper.findById(uploaded.getFileId())).isNotNull();

        setAuth(OTHER_USER_EMAIL, "USER");
        fileService.delete(uploaded.getFileId());
        assertThat(existsInGcs(uploaded.getUploadPath())).isFalse();
    }

    @Test
    @DisplayName("OwnerCheck - 없는 fileId 삭제 → NOT_FOUND")
    void deleteNotFound() {
        setAuth(USER_EMAIL, "USER");
        assertThatThrownBy(() -> fileService.delete(NOT_FOUND_FILE_ID))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("리소스를 찾을 수 없습니다");
    }

    // ── 부모 기준 전체 삭제 + GCS 확인 ────────────────────────────────────

    @Test
    @DisplayName("deleteByParent - PORTFOLIO_FILE 전체 삭제 + GCS 삭제 확인")
    void deleteByParent() throws Exception {
        MockMultipartFile f1 = new MockMultipartFile("files", "parent-1.jpg", "image/jpeg", "c1".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "parent-2.jpg", "image/jpeg", "c2".getBytes());
        List<FileDTO> uploaded = upload(
                USER_EMAIL, "USER", FileParentType.PORTFOLIO_FILE, PARENT_ID, f1, f2);

        fileService.deleteByParent(FileParentType.PORTFOLIO_FILE, PARENT_ID);

        List<FileDTO> remaining = fileService.findByParent(FileParentType.PORTFOLIO_FILE, PARENT_ID);
        assertThat(remaining).isEmpty();
        uploaded.forEach(file -> assertThat(existsInGcs(file.getUploadPath())).isFalse());
    }

    @Test
    @DisplayName("deleteByParent - 파일 없는 parentId → 정상 처리")
    void deleteByParentEmpty() {
        assertThatNoException().isThrownBy(
                () -> fileService.deleteByParent(FileParentType.PORTFOLIO_FILE, PARENT_ID));
    }

    // ── 회사 파일 정리 ────────────────────────────────────────────

    @Test
    @DisplayName("회사 파일 삭제 - 본인 확인 후 삭제 + GCS 삭제 확인")
    void deleteCompanyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "delete-company.jpg", "image/jpeg", "delete-company".getBytes());
        FileDTO uploaded = upload(COMPANY_EMAIL, "COMPANY", FileParentType.PROFILE, null, file).get(0);

        assertThatNoException().isThrownBy(() -> fileService.delete(uploaded.getFileId()));

        assertThat(fileMapper.findById(uploaded.getFileId())).isNull();
        assertThat(existsInGcs(uploaded.getUploadPath())).isFalse();
    }
}
