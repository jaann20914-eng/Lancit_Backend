package com.ssafy.lancit.domain.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.company.service.CompanyService;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.event.FileDeleteEvent;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.OwnerType;

import lombok.extern.slf4j.Slf4j;


@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
 
    @InjectMocks private CompanyService companyService;
 
    @Mock private CompanyMapper companyMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private FileMapper fileMapper;
    @Mock private FileService fileService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;
 
    private CompanyDTO mockCompany;
 
    @BeforeEach
    void setUp() {
        mockCompany = CompanyDTO.builder()
                .email("company@test.com")
                .password("encodedPw")
                .name("담당자")
                .companyName("테스트회사")
                .phone("02-1234-5678")
                .jobCategory(JobCategory.IT)
                .pushable(false)
                .businessNumber("1234567890")
                .businessNumberVerified(true)
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
        	System.out.println("test");
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany);
 
            CompanyDTO result = companyService.getMe("company@test.com");
 
            assertThat(result.getEmail()).isEqualTo("company@test.com");
            assertThat(result.getPassword()).isEmpty();
        }
 
        @Test
        @DisplayName("존재하지 않는 이메일 시 예외")
        void getMe_notFound() {
            given(companyMapper.findByEmail("none@test.com")).willReturn(null);
 
            assertThatThrownBy(() -> companyService.getMe("none@test.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());
        }
 
        @Test
        @DisplayName("비밀번호 응답에 포함되지 않음 확인")
        void getMe_passwordHidden() {
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany);
 
            CompanyDTO result = companyService.getMe("company@test.com");
 
            assertThat(result.getPassword()).isNotEqualTo("encodedPw");
            assertThat(result.getPassword()).isEmpty();
        }
 
        @Test
        @DisplayName("사업자번호 인증 여부 포함 반환")
        void getMe_includesBusinessVerified() {
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany);
 
            CompanyDTO result = companyService.getMe("company@test.com");
 
            assertThat(result.isBusinessNumberVerified()).isTrue();
            assertThat(result.getBusinessNumber()).isEqualTo("1234567890");
        }
    }
 
    // ═══════════════════════════════════════════════════════
    //  update()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("update() 마이페이지 수정")
    class UpdateTest {
 
        private CompanyDTO updateDto;
 
        @BeforeEach
        void setUp() {
            updateDto = CompanyDTO.builder()
                    .email("company@test.com")
                    .name("수정된담당자")
                    .companyName("수정된회사명")
                    .phone("02-9999-0000")
                    .jobCategory(JobCategory.DESIGN)
                    .pushable(true)
                    .build();
        }
 
        @Test
        @DisplayName("존재하지 않는 회사 수정 시 예외")
        void update_notFound() {
            given(companyMapper.findByEmail("company@test.com")).willReturn(null);
 
            assertThatThrownBy(() -> companyService.update(updateDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());
        }
 
        @Test
        @DisplayName("프로필 사진 없이 정보만 수정")
        void update_noProfileChange() {
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany);
 
            companyService.update(updateDto);
 
            verify(companyMapper, times(1)).update(updateDto);
            verify(fileMapper, never()).updateParentType(anyInt(), any());
            verify(fileService, never()).delete(anyInt());
        }
 
        @Test
        @DisplayName("프로필 사진 변경 - 기존 사진 없을 때")
        void update_withNewProfile_noExisting() {
            updateDto.setProfileFileId(10);
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany); // profileFileId=null
 
            companyService.update(updateDto);
 
            verify(fileMapper, times(1)).updateParentType(10, FileParentType.PROFILE);
            verify(fileService, never()).delete(anyInt());
            verify(companyMapper, times(1)).update(updateDto);
        }
 
        @Test
        @DisplayName("프로필 사진 변경 - 기존 사진 있을 때 기존 삭제")
        void update_withNewProfile_existingDeleted() {
            mockCompany.setProfileFileId(5);
            updateDto.setProfileFileId(10);
 
            FileDTO oldFile = FileDTO.builder()
                    .fileId(5).uploadPath("profile/old.jpg").build();
 
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany);
            given(fileMapper.findById(5)).willReturn(oldFile);
 
            companyService.update(updateDto);
 
            verify(fileService, times(1)).delete(5);
            verify(fileMapper, times(1)).updateParentType(10, FileParentType.PROFILE);
            verify(companyMapper, times(1)).update(updateDto);
        }
 
        @Test
        @DisplayName("같은 프로필 사진 ID 로 수정 시 기존 삭제 후 재등록")
        void update_sameProfileId() {
            mockCompany.setProfileFileId(10);
            updateDto.setProfileFileId(10);
 
            FileDTO oldFile = FileDTO.builder()
                    .fileId(10).uploadPath("profile/same.jpg").build();
 
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany);
            given(fileMapper.findById(10)).willReturn(oldFile);
 
            companyService.update(updateDto);
 
            verify(fileService, times(1)).delete(10);
            verify(fileMapper, times(1)).updateParentType(10, FileParentType.PROFILE);
        }
    }
 
    // ═══════════════════════════════════════════════════════
    //  delete()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("delete() 회원 탈퇴")
    class DeleteTest {
 
//        @Test
//        @DisplayName("존재하지 않는 회사 탈퇴 시 예외")
//        void delete_notFound() {
//            given(companyMapper.findByEmail("company@test.com")).willReturn(null);
// 
//            try (var mock = mockStatic(SecurityUtil.class)) {
//                mock.when(SecurityUtil::getCurrentEmail).thenReturn("company@test.com");
// 
//                assertThatThrownBy(() -> companyService.delete())
//                        .isInstanceOf(CustomException.class)
//                        .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());
//            }
//        }
 
        @Test
        @DisplayName("파일 없는 회사 탈퇴 - Task, Category, Company 순서 삭제")
        void delete_noFiles() {
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany);
            given(fileMapper.findByCompanyEmail("company@test.com")).willReturn(List.of());
 
            try (var mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentEmail).thenReturn("company@test.com");
 
                companyService.delete();
 
                verify(taskMapper, times(1)).deleteByOwner("company@test.com", OwnerType.company);
                verify(categoryMapper, times(1)).deleteByOwner("company@test.com", OwnerType.company);
                verify(companyMapper, times(1)).softDelete("company@test.com");
            }
        }
 
        @Test
        @DisplayName("파일 있는 회사 탈퇴 - FileDeleteEvent 발행 + Redis 캐시 제거")
        void delete_withFiles() {
            FileDTO file1 = FileDTO.builder().fileId(1).uploadPath("profile/a.jpg").build();
            FileDTO file2 = FileDTO.builder().fileId(2).uploadPath("contract/b.pdf").build();
 
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany);
            given(fileMapper.findByCompanyEmail("company@test.com")).willReturn(List.of(file1, file2));
 
            try (var mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentEmail).thenReturn("company@test.com");
 
                companyService.delete();
 
                verify(eventPublisher, times(2)).publishEvent(any(FileDeleteEvent.class));
                verify(cache, times(2)).evict(anyInt());
                verify(companyMapper, times(1)).softDelete("company@test.com");
            }
        }
 
        @Test
        @DisplayName("탈퇴 시 CASCADE 대상 확인 - Task, Category 앱 레벨 삭제")
        void delete_appLevelDeletion() {
            given(companyMapper.findByEmail("company@test.com")).willReturn(mockCompany);
            given(fileMapper.findByCompanyEmail("company@test.com")).willReturn(List.of());
 
            try (var mock = mockStatic(SecurityUtil.class)) {
                mock.when(SecurityUtil::getCurrentEmail).thenReturn("company@test.com");
 
                companyService.delete();
 
                // Task, Category 는 FK 없어서 앱 레벨 삭제 필요
                verify(taskMapper).deleteByOwner("company@test.com", OwnerType.company);
                verify(categoryMapper).deleteByOwner("company@test.com", OwnerType.company);
                // Recruitment, Bookmark 등은 CASCADE 자동 처리
                verify(companyMapper).softDelete("company@test.com");
            }
        }
    }
}
