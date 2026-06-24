package com.ssafy.lancit.domain.bookmark.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
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

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;
import com.ssafy.lancit.domain.bookmark.company.dto.TalentListDTO;
import com.ssafy.lancit.domain.bookmark.company.mapper.CompanyBookmarkMapper;
import com.ssafy.lancit.domain.bookmark.company.service.CompanyBookmarkService;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import com.ssafy.lancit.global.enums.JobCategory;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class CompanyBookmarkServiceTest {

    @InjectMocks
    private CompanyBookmarkService bookmarkService;

    @Mock
    private CompanyBookmarkMapper companyBookmarkMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private FileService fileService;

    private PageRequest pageRequest;
    private CompanyBookmarkDTO mockBookmark;

    @BeforeEach
    void setUp() {
        pageRequest = new PageRequest();
        pageRequest.setPage(1);
        pageRequest.setSize(10);
        pageRequest.setSort("created_at");
        pageRequest.setDirection("DESC");

        mockBookmark = CompanyBookmarkDTO.builder()
                .bookmarkId(1)
                .companyEmail("company@test.com")
                .freelancerEmail("user@test.com")
                .applicationId(null)
                .bookmarkedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("create() 찜 추가")
    class CreateTest {

        @Test
        @DisplayName("정상 찜 추가 성공")
        void create_success() {
            CompanyBookmarkDTO dto = new CompanyBookmarkDTO();
            dto.setFreelancerEmail("user@test.com");
            given(companyBookmarkMapper.exists("company@test.com", "user@test.com"))
                    .willReturn(false);

            bookmarkService.create(dto, "company@test.com");

            verify(companyBookmarkMapper).insert(dto);
        }

        @Test
        @DisplayName("중복 찜 시 INVALID_INPUT 예외")
        void create_duplicate() {
            CompanyBookmarkDTO dto = new CompanyBookmarkDTO();
            dto.setFreelancerEmail("user@test.com");
            given(companyBookmarkMapper.exists("company@test.com", "user@test.com"))
                    .willReturn(true);

            assertThatThrownBy(() -> bookmarkService.create(dto, "company@test.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.INVALID_INPUT.getMessage());

            verify(companyBookmarkMapper, never()).insert(any());
        }

        @Test
        @DisplayName("찜 추가 시 companyEmail 자동 세팅")
        void create_companyEmailAutoSet() {
            CompanyBookmarkDTO dto = new CompanyBookmarkDTO();
            dto.setFreelancerEmail("user@test.com");
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            bookmarkService.create(dto, "company@test.com");

            assertThat(dto.getCompanyEmail()).isEqualTo("company@test.com");
        }

        @Test
        @DisplayName("insert 정확히 1회만 호출")
        void create_insertOnce() {
            CompanyBookmarkDTO dto = new CompanyBookmarkDTO();
            dto.setFreelancerEmail("user@test.com");
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            bookmarkService.create(dto, "company@test.com");

            verify(companyBookmarkMapper, times(1)).insert(any());
        }
    }

    @Nested
    @DisplayName("deleteByFreelancer() 찜 취소")
    class DeleteByFreelancerTest {

        @Test
        @DisplayName("정상 찜 취소 성공")
        void delete_success() {
            given(companyBookmarkMapper.findByCompanyAndFreelancer("company@test.com", "user@test.com"))
                    .willReturn(mockBookmark);

            bookmarkService.deleteByFreelancer("company@test.com", "user@test.com");

            verify(companyBookmarkMapper).delete(1);
        }

        @Test
        @DisplayName("회사와 프리랜서 조합이 없으면 NOT_FOUND")
        void delete_notFound() {
            given(companyBookmarkMapper.findByCompanyAndFreelancer("company@test.com", "missing@test.com"))
                    .willReturn(null);

            assertThatThrownBy(() ->
                    bookmarkService.deleteByFreelancer("company@test.com", "missing@test.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());

            verify(companyBookmarkMapper, never()).delete(anyInt());
        }

        @Test
        @DisplayName("찾은 bookmarkId로 delete 호출")
        void delete_usesBookmarkId() {
            CompanyBookmarkDTO bookmark = CompanyBookmarkDTO.builder()
                    .bookmarkId(77)
                    .companyEmail("company@test.com")
                    .freelancerEmail("user@test.com")
                    .build();
            given(companyBookmarkMapper.findByCompanyAndFreelancer(anyString(), anyString()))
                    .willReturn(bookmark);

            bookmarkService.deleteByFreelancer("company@test.com", "user@test.com");

            verify(companyBookmarkMapper).delete(77);
        }
    }

    @Nested
    @DisplayName("searchTalents() 프리랜서 목록 조회")
    class SearchTalentsTest {

        private TalentListDTO talent1;
        private TalentListDTO talent2;

        @BeforeEach
        void setUp() {
            talent1 = TalentListDTO.builder()
                    .email("user1@test.com")
                    .name("홍길동")
                    .jobCategory(JobCategory.IT)
                    .profileFileId(10)
                    .bookmarked(true)
                    .build();
            talent2 = TalentListDTO.builder()
                    .email("user2@test.com")
                    .name("김철수")
                    .jobCategory(JobCategory.DESIGN)
                    .bookmarked(false)
                    .build();
        }

        @Test
        @DisplayName("전체 목록 조회 성공")
        void search_all() {
            given(companyBookmarkMapper.searchTalents(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(talent1, talent2));
            given(companyBookmarkMapper.countTalents(any(), any(), anyBoolean(), anyString()))
                    .willReturn(2L);
            given(fileService.getSignedUrl(10)).willReturn("https://signed.example/profile.png");

            PageResponse<TalentListDTO> result = bookmarkService.searchTalents(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2L);
            assertThat(result.getContent().get(0).getProfileImageUrl())
                    .isEqualTo("https://signed.example/profile.png");
        }

        @Test
        @DisplayName("bookmarked=true 시 찜한 프리랜서만 조회")
        void search_bookmarkedOnly() {
            given(companyBookmarkMapper.searchTalents(
                    any(), any(), eq(true), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(talent1));
            given(companyBookmarkMapper.countTalents(any(), any(), eq(true), anyString()))
                    .willReturn(1L);
            given(fileService.getSignedUrl(10)).willReturn("https://signed.example/profile.png");

            PageResponse<TalentListDTO> result = bookmarkService.searchTalents(
                    "company@test.com", null, JobCategory.IT, true, pageRequest);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).isBookmarked()).isTrue();
        }

        @Test
        @DisplayName("키워드와 카테고리를 mapper에 전달")
        void search_keywordAndCategory() {
            given(companyBookmarkMapper.searchTalents(
                    eq("홍길동"), eq(JobCategory.IT), anyBoolean(), eq("company@test.com"),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(talent1));
            given(companyBookmarkMapper.countTalents(
                    eq("홍길동"), eq(JobCategory.IT), anyBoolean(), eq("company@test.com")))
                    .willReturn(1L);
            given(fileService.getSignedUrl(10)).willReturn("https://signed.example/profile.png");

            PageResponse<TalentListDTO> result = bookmarkService.searchTalents(
                    "company@test.com", "홍길동", JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("키워드 null 시 null 그대로 전달")
        void search_nullKeyword() {
            given(companyBookmarkMapper.searchTalents(
                    isNull(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(talent1, talent2));
            given(companyBookmarkMapper.countTalents(isNull(), any(), anyBoolean(), anyString()))
                    .willReturn(2L);
            given(fileService.getSignedUrl(10)).willReturn("https://signed.example/profile.png");

            PageResponse<TalentListDTO> result = bookmarkService.searchTalents(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("검색 결과 없을 때 빈 리스트")
        void search_empty() {
            given(companyBookmarkMapper.searchTalents(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(companyBookmarkMapper.countTalents(any(), any(), anyBoolean(), anyString()))
                    .willReturn(0L);

            PageResponse<TalentListDTO> result = bookmarkService.searchTalents(
                    "company@test.com", "없는사람", JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0L);
        }

        @Test
        @DisplayName("sort=name 시 getSafeSort=name, getSafeDirection=ASC 전달")
        void search_sortByName() {
            pageRequest.setSort("name");
            pageRequest.setDirection("ASC");

            given(companyBookmarkMapper.searchTalents(
                    any(), any(), anyBoolean(), anyString(),
                    eq("name"), eq("ASC"), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(companyBookmarkMapper.countTalents(any(), any(), anyBoolean(), anyString()))
                    .willReturn(0L);

            bookmarkService.searchTalents(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            verify(companyBookmarkMapper).searchTalents(
                    any(), any(), anyBoolean(), anyString(),
                    eq("name"), eq("ASC"), anyInt(), anyInt());
        }

        @Test
        @DisplayName("SQL 인젝션 시도 시 getSafeSort 기본값 created_at 으로 대체")
        void search_sqlInjection_safeSort() {
            pageRequest.setSort("DROP TABLE user");

            given(companyBookmarkMapper.searchTalents(
                    any(), any(), anyBoolean(), anyString(),
                    eq("created_at"), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(companyBookmarkMapper.countTalents(any(), any(), anyBoolean(), anyString()))
                    .willReturn(0L);

            bookmarkService.searchTalents(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            verify(companyBookmarkMapper).searchTalents(
                    any(), any(), anyBoolean(), anyString(),
                    eq("created_at"), anyString(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("direction=asc 소문자 시 ASC 로 정규화")
        void search_directionLowercase() {
            pageRequest.setDirection("asc");

            given(companyBookmarkMapper.searchTalents(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), eq("ASC"), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(companyBookmarkMapper.countTalents(any(), any(), anyBoolean(), anyString()))
                    .willReturn(0L);

            bookmarkService.searchTalents(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            verify(companyBookmarkMapper).searchTalents(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), eq("ASC"), anyInt(), anyInt());
        }

        @Test
        @DisplayName("profileFileId가 없으면 signed URL 조회하지 않음")
        void search_noProfileFile_noSignedUrlLookup() {
            given(companyBookmarkMapper.searchTalents(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(talent2));
            given(companyBookmarkMapper.countTalents(any(), any(), anyBoolean(), anyString()))
                    .willReturn(1L);

            PageResponse<TalentListDTO> result = bookmarkService.searchTalents(
                    "company@test.com", null, JobCategory.DESIGN, false, pageRequest);

            assertThat(result.getContent()).containsExactly(talent2);
            verify(fileService, never()).getSignedUrl(anyInt());
        }
    }
}
