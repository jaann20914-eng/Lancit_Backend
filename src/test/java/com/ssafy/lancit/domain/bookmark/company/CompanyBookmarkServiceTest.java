package com.ssafy.lancit.domain.bookmark.company;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;
import com.ssafy.lancit.domain.bookmark.company.mapper.CompanyBookmarkMapper;
import com.ssafy.lancit.domain.bookmark.company.service.CompanyBookmarkService;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import com.ssafy.lancit.global.enums.JobCategory;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class CompanyBookmarkServiceTest {

    @InjectMocks private CompanyBookmarkService bookmarkService;

    @Mock private CompanyBookmarkMapper companyBookmarkMapper;
    @Mock private UserMapper userMapper;
    @Mock private CompanyMapper companyMapper;

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

    // ═══════════════════════════════════════════════════════
    //  getList()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("getList() 찜 목록 조회")
    class GetListTest {

        @Test
        @DisplayName("정상 찜 목록 반환")
        void getList_success() {
            given(companyBookmarkMapper.findByCompany("company@test.com", 0, 10))
                    .willReturn(List.of(mockBookmark));
            given(companyBookmarkMapper.countByCompany("company@test.com")).willReturn(1L);

            PageResponse<CompanyBookmarkDTO> result =
                    bookmarkService.getList("company@test.com", pageRequest);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getTotalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("찜 목록 없을 때 빈 리스트 반환")
        void getList_empty() {
            given(companyBookmarkMapper.findByCompany(anyString(), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(companyBookmarkMapper.countByCompany(anyString())).willReturn(0L);

            PageResponse<CompanyBookmarkDTO> result =
                    bookmarkService.getList("company@test.com", pageRequest);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0L);
            assertThat(result.getTotalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("2페이지 요청 시 offset 10으로 조회")
        void getList_page2_offset() {
            pageRequest.setPage(2);
            given(companyBookmarkMapper.findByCompany("company@test.com", 10, 10))
                    .willReturn(List.of());
            given(companyBookmarkMapper.countByCompany(anyString())).willReturn(15L);

            bookmarkService.getList("company@test.com", pageRequest);

            verify(companyBookmarkMapper).findByCompany("company@test.com", 10, 10);
        }

        @Test
        @DisplayName("마지막 페이지 - hasNext=false")
        void getList_lastPage_noNext() {
            pageRequest.setPage(3);
            pageRequest.setSize(10);
            given(companyBookmarkMapper.findByCompany(anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(mockBookmark));
            given(companyBookmarkMapper.countByCompany(anyString())).willReturn(25L);

            PageResponse<CompanyBookmarkDTO> result =
                    bookmarkService.getList("company@test.com", pageRequest);

            assertThat(result.isHasNext()).isFalse();
            assertThat(result.isHasPrev()).isTrue();
        }

        @Test
        @DisplayName("첫 번째 페이지 - hasPrev=false")
        void getList_firstPage_noPrev() {
            given(companyBookmarkMapper.findByCompany(anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(mockBookmark));
            given(companyBookmarkMapper.countByCompany(anyString())).willReturn(25L);

            PageResponse<CompanyBookmarkDTO> result =
                    bookmarkService.getList("company@test.com", pageRequest);

            assertThat(result.isHasPrev()).isFalse();
            assertThat(result.isHasNext()).isTrue();
        }

        @Test
        @DisplayName("totalPages 정확히 계산 - 25개 / 10개씩 = 3페이지")
        void getList_totalPagesCalc() {
            given(companyBookmarkMapper.findByCompany(anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(mockBookmark));
            given(companyBookmarkMapper.countByCompany(anyString())).willReturn(25L);

            PageResponse<CompanyBookmarkDTO> result =
                    bookmarkService.getList("company@test.com", pageRequest);

            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("totalPages 정확히 계산 - 20개 / 10개씩 = 2페이지 (딱 나눠떨어짐)")
        void getList_totalPagesExact() {
            given(companyBookmarkMapper.findByCompany(anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(mockBookmark));
            given(companyBookmarkMapper.countByCompany(anyString())).willReturn(20L);

            PageResponse<CompanyBookmarkDTO> result =
                    bookmarkService.getList("company@test.com", pageRequest);

            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("size=5 로 요청 시 offset 정확히 계산")
        void getList_customSize() {
            pageRequest.setPage(3);
            pageRequest.setSize(5);
            given(companyBookmarkMapper.findByCompany("company@test.com", 10, 5))
                    .willReturn(List.of());
            given(companyBookmarkMapper.countByCompany(anyString())).willReturn(0L);

            bookmarkService.getList("company@test.com", pageRequest);

            verify(companyBookmarkMapper).findByCompany("company@test.com", 10, 5);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  create()
    // ═══════════════════════════════════════════════════════
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
        @DisplayName("찜 추가 시 companyEmail 자동 세팅 확인")
        void create_companyEmailAutoSet() {
            CompanyBookmarkDTO dto = new CompanyBookmarkDTO();
            dto.setFreelancerEmail("user@test.com");
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            bookmarkService.create(dto, "company@test.com");

            assertThat(dto.getCompanyEmail()).isEqualTo("company@test.com");
        }

        @Test
        @DisplayName("applicationId null 로 직접 찜 추가")
        void create_directBookmark_applicationIdNull() {
            CompanyBookmarkDTO dto = new CompanyBookmarkDTO();
            dto.setFreelancerEmail("user@test.com");
            dto.setApplicationId(null);
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            bookmarkService.create(dto, "company@test.com");

            verify(companyBookmarkMapper).insert(argThat(d -> d.getApplicationId() == null));
        }

        @Test
        @DisplayName("applicationId 있을 때 지원에서 찜 추가")
        void create_fromApplication() {
            CompanyBookmarkDTO dto = new CompanyBookmarkDTO();
            dto.setFreelancerEmail("user@test.com");
            dto.setApplicationId(5);
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            bookmarkService.create(dto, "company@test.com");

            verify(companyBookmarkMapper).insert(argThat(d -> d.getApplicationId() == 5));
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

    // ═══════════════════════════════════════════════════════
    //  delete()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("delete() 찜 취소")
    class DeleteTest {

        @Test
        @DisplayName("정상 찜 취소 성공")
        void delete_success() {
            given(companyBookmarkMapper.findById(1)).willReturn(mockBookmark);

            bookmarkService.delete(1, "company@test.com");

            verify(companyBookmarkMapper).delete(1);
        }

        @Test
        @DisplayName("존재하지 않는 bookmarkId 시 NOT_FOUND 예외")
        void delete_notFound() {
            given(companyBookmarkMapper.findById(999)).willReturn(null);

            assertThatThrownBy(() -> bookmarkService.delete(999, "company@test.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());

            verify(companyBookmarkMapper, never()).delete(anyInt());
        }

        @Test
        @DisplayName("타인의 찜 취소 시 FORBIDDEN 예외")
        void delete_forbidden() {
            given(companyBookmarkMapper.findById(1)).willReturn(mockBookmark);

            assertThatThrownBy(() -> bookmarkService.delete(1, "other@test.com"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.FORBIDDEN.getMessage());

            verify(companyBookmarkMapper, never()).delete(anyInt());
        }

        @Test
        @DisplayName("delete 정확히 1회만 호출")
        void delete_calledOnce() {
            given(companyBookmarkMapper.findById(1)).willReturn(mockBookmark);

            bookmarkService.delete(1, "company@test.com");

            verify(companyBookmarkMapper, times(1)).delete(1);
        }

        @Test
        @DisplayName("NOT_FOUND 시 delete 호출 안 됨")
        void delete_notFound_noDeleteCall() {
            given(companyBookmarkMapper.findById(999)).willReturn(null);

            try { bookmarkService.delete(999, "company@test.com"); } catch (Exception ignored) {}

            verify(companyBookmarkMapper, never()).delete(anyInt());
        }

        @Test
        @DisplayName("FORBIDDEN 시 delete 호출 안 됨")
        void delete_forbidden_noDeleteCall() {
            given(companyBookmarkMapper.findById(1)).willReturn(mockBookmark);

            try { bookmarkService.delete(1, "hacker@test.com"); } catch (Exception ignored) {}

            verify(companyBookmarkMapper, never()).delete(anyInt());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  searchFreelancers()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("searchFreelancers() 프리랜서 목록 조회")
    class SearchFreelancersTest {

        private UserDTO user1;
        private UserDTO user2;

        @BeforeEach
        void setUp() {
            user1 = UserDTO.builder().email("user1@test.com").name("홍길동")
                    .jobCategory(JobCategory.IT).build();
            user2 = UserDTO.builder().email("user2@test.com").name("김철수")
                    .jobCategory(JobCategory.DESIGN).build();
        }

        @Test
        @DisplayName("전체 목록 조회 성공")
        void search_all() {
            given(userMapper.searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(user1, user2));
            given(userMapper.countFreelancers(any(), any(), anyBoolean(), anyString()))
                    .willReturn(2L);
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            PageResponse<UserDTO> result = bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("bookmarked=true 시 찜한 프리랜서만 조회")
        void search_bookmarkedOnly() {
            given(userMapper.searchFreelancers(
                    any(), any(), eq(true), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(user1));
            given(userMapper.countFreelancers(any(), any(), eq(true), anyString()))
                    .willReturn(1L);
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(true);

            PageResponse<UserDTO> result = bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, true, pageRequest);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).isBookmarked()).isTrue();
        }

        @Test
        @DisplayName("키워드 검색 - 이름 일치")
        void search_keyword() {
            given(userMapper.searchFreelancers(
                    eq("홍길동"), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(user1));
            given(userMapper.countFreelancers(
                    eq("홍길동"), any(), anyBoolean(), anyString()))
                    .willReturn(1L);
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            PageResponse<UserDTO> result = bookmarkService.searchFreelancers(
                    "company@test.com", "홍길동", JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("키워드 null 시 전체 조회")
        void search_nullKeyword() {
            given(userMapper.searchFreelancers(
                    isNull(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(user1, user2));
            given(userMapper.countFreelancers(isNull(), any(), anyBoolean(), anyString()))
                    .willReturn(2L);
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            PageResponse<UserDTO> result = bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("검색 결과 없을 때 빈 리스트")
        void search_empty() {
            given(userMapper.searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(userMapper.countFreelancers(any(), any(), anyBoolean(), anyString()))
                    .willReturn(0L);

            PageResponse<UserDTO> result = bookmarkService.searchFreelancers(
                    "company@test.com", "없는사람", JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0L);
        }

        @Test
        @DisplayName("isBookmarked 찜 여부 프리랜서별 정확히 세팅")
        void search_bookmarkFlag_perUser() {
            given(userMapper.searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(user1, user2));
            given(userMapper.countFreelancers(any(), any(), anyBoolean(), anyString()))
                    .willReturn(2L);
            given(companyBookmarkMapper.exists("company@test.com", "user1@test.com"))
                    .willReturn(true);
            given(companyBookmarkMapper.exists("company@test.com", "user2@test.com"))
                    .willReturn(false);

            PageResponse<UserDTO> result = bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            assertThat(result.getContent().get(0).isBookmarked()).isTrue();
            assertThat(result.getContent().get(1).isBookmarked()).isFalse();
        }

        @Test
        @DisplayName("전원 찜한 경우 isBookmarked 전부 true")
        void search_allBookmarked() {
            given(userMapper.searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(user1, user2));
            given(userMapper.countFreelancers(any(), any(), anyBoolean(), anyString()))
                    .willReturn(2L);
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(true);

            PageResponse<UserDTO> result = bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).allMatch(UserDTO::isBookmarked);
        }

        @Test
        @DisplayName("전원 찜 안 한 경우 isBookmarked 전부 false")
        void search_noneBookmarked() {
            given(userMapper.searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(user1, user2));
            given(userMapper.countFreelancers(any(), any(), anyBoolean(), anyString()))
                    .willReturn(2L);
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            PageResponse<UserDTO> result = bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            assertThat(result.getContent()).noneMatch(UserDTO::isBookmarked);
        }

        @Test
        @DisplayName("sort=name 시 getSafeSort=name, getSafeDirection=ASC 으로 전달")
        void search_sortByName() {
            pageRequest.setSort("name");
            pageRequest.setDirection("ASC");

            given(userMapper.searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    eq("name"), eq("ASC"), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(userMapper.countFreelancers(any(), any(), anyBoolean(), anyString()))
                    .willReturn(0L);

            bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            verify(userMapper).searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    eq("name"), eq("ASC"), anyInt(), anyInt());
        }

        @Test
        @DisplayName("SQL 인젝션 시도 시 getSafeSort 기본값 created_at 으로 대체")
        void search_sqlInjection_safeSort() {
            pageRequest.setSort("DROP TABLE user");

            given(userMapper.searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    eq("created_at"), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(userMapper.countFreelancers(any(), any(), anyBoolean(), anyString()))
                    .willReturn(0L);

            bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            verify(userMapper).searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    eq("created_at"), anyString(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("direction=asc 소문자 시 ASC 로 정규화")
        void search_directionLowercase() {
            pageRequest.setDirection("asc");

            given(userMapper.searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), eq("ASC"), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(userMapper.countFreelancers(any(), any(), anyBoolean(), anyString()))
                    .willReturn(0L);

            bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            verify(userMapper).searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), eq("ASC"), anyInt(), anyInt());
        }

        @Test
        @DisplayName("exists 호출 횟수 - 조회된 프리랜서 수만큼 호출")
        void search_existsCalledPerUser() {
            given(userMapper.searchFreelancers(
                    any(), any(), anyBoolean(), anyString(),
                    anyString(), anyString(), anyInt(), anyInt()))
                    .willReturn(List.of(user1, user2));
            given(userMapper.countFreelancers(any(), any(), anyBoolean(), anyString()))
                    .willReturn(2L);
            given(companyBookmarkMapper.exists(anyString(), anyString())).willReturn(false);

            bookmarkService.searchFreelancers(
                    "company@test.com", null, JobCategory.IT, false, pageRequest);

            verify(companyBookmarkMapper, times(2)).exists(anyString(), anyString());
        }
    }
}