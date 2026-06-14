package com.ssafy.lancit.domain.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
 
import java.util.List;
 
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
 
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.event.FileDeleteEvent;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import com.ssafy.lancit.domain.user.service.UserService;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.OwnerType;


@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
 
    @InjectMocks private UserService userService;
 
    @Mock private UserMapper userMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private FileMapper fileMapper;
    @Mock private FileService fileService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;
 
    private UserDTO mockUser;
 
    @BeforeEach
    void setUp() {
        mockUser = UserDTO.builder()
                .email("user@test.com")
                .password("encodedPw")
                .name("홍길동")
                .phone("010-1234-5678")
                .jobCategory(JobCategory.IT)
                .pushable(false)
                .profileFileId(null)
                .build();
 
        given(cacheManager.getCache("signedUrl")).willReturn(cache);
    }
 
    // ═══════════════════════════════════════════════════════
    //  getMe()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("getMe() 마이페이지 조회")
    class GetMeTest {
 
        @Test
        @DisplayName("정상 조회 - 비밀번호 빈 문자열로 반환")
        void getMe_success() {
            given(userMapper.findByEmail("user@test.com")).willReturn(mockUser);
 
            UserDTO result = userService.getMe("user@test.com");
 
            assertThat(result.getEmail()).isEqualTo("user@test.com");
            assertThat(result.getPassword()).isEmpty();
        }
 
        @Test
        @DisplayName("존재하지 않는 이메일 시 예외")
        void getMe_notFound() {
            given(userMapper.findByEmail("none@test.com")).willReturn(null);
 
            assertThatThrownBy(() -> userService.getMe("none@test.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());
        }
 
        @Test
        @DisplayName("비밀번호 응답에 포함되지 않음 확인")
        void getMe_passwordHidden() {
            given(userMapper.findByEmail("user@test.com")).willReturn(mockUser);
 
            UserDTO result = userService.getMe("user@test.com");
 
            assertThat(result.getPassword()).isNotEqualTo("encodedPw");
            assertThat(result.getPassword()).isEmpty();
        }
    }
 
    // ═══════════════════════════════════════════════════════
    //  update()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("update() 마이페이지 수정")
    class UpdateTest {
 
        private UserDTO updateDto;
 
        @BeforeEach
        void setUp() {
            updateDto = UserDTO.builder()
                    .email("user@test.com")
                    .name("수정된이름")
                    .phone("010-9999-0000")
                    .jobCategory(JobCategory.DESIGN)
                    .pushable(true)
                    .build();
        }
 
        @Test
        @DisplayName("존재하지 않는 유저 수정 시 예외")
        void update_notFound() {
            given(userMapper.findByEmail("user@test.com")).willReturn(null);
 
            assertThatThrownBy(() -> userService.update(updateDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());
        }
 
        @Test
        @DisplayName("프로필 사진 없이 정보만 수정")
        void update_noProfileChange() {
            given(userMapper.findByEmail("user@test.com")).willReturn(mockUser);
 
            userService.update(updateDto);
 
            verify(userMapper, times(1)).update(updateDto);
            verify(fileMapper, never()).updateParentType(anyInt(), any());
            verify(fileService, never()).delete(anyInt());
        }
 
        @Test
        @DisplayName("프로필 사진 변경 - 기존 사진 없을 때")
        void update_withNewProfile_noExisting() {
            updateDto.setProfileFileId(10);
            given(userMapper.findByEmail("user@test.com")).willReturn(mockUser); // profileFileId=null
 
            userService.update(updateDto);
 
            verify(fileService, times(1)).promote(10, FileParentType.PROFILE);
            verify(fileService, never()).delete(anyInt());
            verify(userMapper, times(1)).update(updateDto);
        }
 
        @Test
        @DisplayName("프로필 사진 변경 - 기존 사진 있을 때 기존 삭제")
        void update_withNewProfile_existingDeleted() {
            mockUser.setProfileFileId(5);
            updateDto.setProfileFileId(10);
 
            FileDTO oldFile = FileDTO.builder()
                    .fileId(5).uploadPath("profile/old.jpg").build();
 
            given(userMapper.findByEmail("user@test.com")).willReturn(mockUser);
            given(fileMapper.findById(5)).willReturn(oldFile);
 
            userService.update(updateDto);
 
            verify(fileService, times(1)).delete(5);
            verify(fileService, times(1)).promote(10, FileParentType.PROFILE);
            verify(userMapper, times(1)).update(updateDto);
        }
 
        @Test
        @DisplayName("같은 프로필 사진 ID 로 수정 시 기존 삭제 후 재등록")
        void update_sameProfileId() {
            mockUser.setProfileFileId(10);
            updateDto.setProfileFileId(10);
 
            FileDTO oldFile = FileDTO.builder()
                    .fileId(10).uploadPath("profile/same.jpg").build();
 
            given(userMapper.findByEmail("user@test.com")).willReturn(mockUser);
            given(fileMapper.findById(10)).willReturn(oldFile);
 
            userService.update(updateDto);
 
            verify(fileService, times(1)).delete(10);
            verify(fileService, times(1)).promote(10, FileParentType.PROFILE);
        }
    }
 
    // ═══════════════════════════════════════════════════════
    //  delete()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("delete() 회원 탈퇴")
    class DeleteTest {
 
//        @Test
//        @DisplayName("존재하지 않는 유저 탈퇴 시 예외")
//        void delete_notFound() {
//            given(userMapper.findByEmail("user@test.com")).willReturn(null);
// 
//            try (var mock = mockStatic(com.ssafy.lancit.common.util.SecurityUtil.class)) {
//                mock.when(com.ssafy.lancit.common.util.SecurityUtil::getCurrentEmail)
//                    .thenReturn("user@test.com");
// 
//                assertThatThrownBy(() -> userService.delete())
//                        .isInstanceOf(CustomException.class)
//                        .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());
//            }
//        }
 
        @Test
        @DisplayName("파일 없는 유저 탈퇴 - Task, Category, User 순서 삭제")
        void delete_noFiles() {
            given(userMapper.findByEmail("user@test.com")).willReturn(mockUser);
            given(fileMapper.findByUserEmail("user@test.com")).willReturn(List.of());
 
            try (var mock = mockStatic(com.ssafy.lancit.common.util.SecurityUtil.class)) {
                mock.when(com.ssafy.lancit.common.util.SecurityUtil::getCurrentEmail)
                    .thenReturn("user@test.com");
 
                userService.delete();
 
                verify(taskMapper, times(1)).deleteByOwner("user@test.com", OwnerType.USER);
                verify(categoryMapper, times(1)).deleteByOwner("user@test.com", OwnerType.USER);
                verify(userMapper, times(1)).softDelete("user@test.com");
            }
        }
 
        @Test
        @DisplayName("파일 있는 유저 탈퇴 - PROFILE/TEMP만 FileDeleteEvent 발행, 나머지 제외")
        void delete_withFiles() {
            FileDTO file1 = FileDTO.builder().fileId(1).uploadPath("profile/a.jpg")
                    .parentType(FileParentType.PROFILE).build();
            FileDTO file2 = FileDTO.builder().fileId(2).uploadPath("portfolio/b.jpg")
                    .parentType(FileParentType.PORTFOLIO_FILE).build(); // 제외 대상
            FileDTO file3 = FileDTO.builder().fileId(3).uploadPath("temp/c.jpg")
                    .parentType(FileParentType.TEMP).build();

            given(userMapper.findByEmail("user@test.com")).willReturn(mockUser);
            given(fileMapper.findByUserEmail("user@test.com"))
                    .willReturn(List.of(file1, file2, file3));

            try (var mock = mockStatic(com.ssafy.lancit.common.util.SecurityUtil.class)) {
                mock.when(com.ssafy.lancit.common.util.SecurityUtil::getCurrentEmail)
                        .thenReturn("user@test.com");

                userService.delete();

                // file1(PROFILE), file3(TEMP)만 이벤트 발행 - file2(PORTFOLIO_FILE)는 제외
                verify(eventPublisher, times(2)).publishEvent(any(FileDeleteEvent.class));
                verify(userMapper, times(1)).softDelete("user@test.com");
            }
        }
    }
}