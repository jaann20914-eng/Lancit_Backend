package com.ssafy.lancit.domain.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.portfolio.controller.PortfolioController;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileUpdateRequest;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioSearchCondition;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioProfileMapper;
import com.ssafy.lancit.domain.portfolio.service.PortfolioService;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.JobCategory;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    private static final String USER_EMAIL = "user@test.com";

    @InjectMocks
    private PortfolioService portfolioService;

    @Mock
    private PortfolioMapper portfolioMapper;

    @Mock
    private PortfolioProfileMapper portfolioProfileMapper;

    @Mock
    private FileService fileService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("내 포트폴리오 목록 조회 성공")
    void getMyList_success() {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(2);
        pageRequest.setSize(5);
        PortfolioDTO portfolio = basePortfolio(1);

        given(portfolioMapper.findByEmail(USER_EMAIL, pageRequest, null)).willReturn(List.of(portfolio));
        given(portfolioMapper.countByEmail(USER_EMAIL, null)).willReturn(1L);

        PageResponse<PortfolioDTO> result = portfolioService.getMyList(USER_EMAIL, pageRequest);

        assertThat(result.getContent()).containsExactly(portfolio);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(portfolioMapper).findByEmail(USER_EMAIL, pageRequest, null);
        verify(portfolioMapper).countByEmail(USER_EMAIL, null);
    }

    @Test
    @DisplayName("내 포트폴리오 목록 검색 조건 전달")
    void getMyList_withSearchCondition_success() {
        PageRequest pageRequest = new PageRequest();
        PortfolioSearchCondition condition = new PortfolioSearchCondition();
        condition.setKeyword(" 랜딩 ");
        condition.setCategory("design");
        condition.setVisibility("public");
        PortfolioDTO portfolio = basePortfolio(1);

        given(portfolioMapper.findByEmail(USER_EMAIL, pageRequest, condition)).willReturn(List.of(portfolio));
        given(portfolioMapper.countByEmail(USER_EMAIL, condition)).willReturn(1L);

        PageResponse<PortfolioDTO> result = portfolioService.getMyList(USER_EMAIL, pageRequest, condition);

        assertThat(result.getContent()).containsExactly(portfolio);
        assertThat(condition.getKeyword()).isEqualTo("랜딩");
        assertThat(condition.getCategory()).isEqualTo("DESIGN");
        assertThat(condition.getVisibility()).isEqualTo("PUBLIC");
        verify(portfolioMapper).findByEmail(USER_EMAIL, pageRequest, condition);
        verify(portfolioMapper).countByEmail(USER_EMAIL, condition);
    }

    @Test
    @DisplayName("내 포트폴리오 목록 검색 조건이 공백이면 필터 없이 조회")
    void getMyList_blankSearchCondition_success() {
        PageRequest pageRequest = new PageRequest();
        PortfolioSearchCondition condition = new PortfolioSearchCondition();
        condition.setKeyword("   ");
        condition.setCategory("   ");
        condition.setVisibility("unknown");

        given(portfolioMapper.findByEmail(USER_EMAIL, pageRequest, condition)).willReturn(List.of());
        given(portfolioMapper.countByEmail(USER_EMAIL, condition)).willReturn(0L);

        PageResponse<PortfolioDTO> result = portfolioService.getMyList(USER_EMAIL, pageRequest, condition);

        assertThat(result.getContent()).isEmpty();
        assertThat(condition.getKeyword()).isNull();
        assertThat(condition.getCategory()).isNull();
        assertThat(condition.getVisibility()).isNull();
        verify(portfolioMapper).findByEmail(USER_EMAIL, pageRequest, condition);
        verify(portfolioMapper).countByEmail(USER_EMAIL, condition);
    }

    @Test
    @DisplayName("공개 포트폴리오 목록 조회 성공")
    void getPublicList_success() {
        PageRequest pageRequest = new PageRequest();
        PortfolioDTO portfolio = basePortfolio(1);

        given(portfolioMapper.findPublicByEmail(USER_EMAIL, pageRequest, null)).willReturn(List.of(portfolio));
        given(portfolioMapper.countPublicByEmail(USER_EMAIL, null)).willReturn(1L);

        PageResponse<PortfolioDTO> result = portfolioService.getPublicList(USER_EMAIL, pageRequest);

        assertThat(result.getContent()).containsExactly(portfolio);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(portfolioMapper).findPublicByEmail(USER_EMAIL, pageRequest, null);
        verify(portfolioMapper).countPublicByEmail(USER_EMAIL, null);
    }

    @Test
    @DisplayName("공개 포트폴리오 목록 검색 조건 전달")
    void getPublicList_withSearchCondition_success() {
        PageRequest pageRequest = new PageRequest();
        PortfolioSearchCondition condition = new PortfolioSearchCondition();
        condition.setKeyword("앱");
        condition.setCategory("web_app");
        condition.setVisibility("PRIVATE");
        PortfolioDTO portfolio = basePortfolio(1);

        given(portfolioMapper.findPublicByEmail(USER_EMAIL, pageRequest, condition)).willReturn(List.of(portfolio));
        given(portfolioMapper.countPublicByEmail(USER_EMAIL, condition)).willReturn(1L);

        PageResponse<PortfolioDTO> result = portfolioService.getPublicList(USER_EMAIL, pageRequest, condition);

        assertThat(result.getContent()).containsExactly(portfolio);
        assertThat(condition.getKeyword()).isEqualTo("앱");
        assertThat(condition.getCategory()).isEqualTo("WEB_APP");
        assertThat(condition.getVisibility()).isEqualTo("PRIVATE");
        verify(portfolioMapper).findPublicByEmail(USER_EMAIL, pageRequest, condition);
        verify(portfolioMapper).countPublicByEmail(USER_EMAIL, condition);
    }

    @Test
    @DisplayName("내 포트폴리오 프로필 row가 없으면 기본값으로 생성 후 조회")
    void getMyProfile_createDefault_success() {
        PortfolioProfileDTO profile = profile(false, "");
        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(null, profile);
        given(portfolioProfileMapper.insertProfileFromUser(USER_EMAIL)).willReturn(1);
        given(portfolioProfileMapper.findTechStacks(USER_EMAIL)).willReturn(List.of());

        PortfolioProfileDTO result = portfolioService.getMyProfile(USER_EMAIL);

        assertThat(result.getIsPortfolioPublic()).isFalse();
        assertThat(result.getIntro()).isEmpty();
        assertThat(result.getTechStacks()).isEmpty();
        assertThat(result.getDisplayName()).isEqualTo("홍길동");
        verify(portfolioProfileMapper).insertProfileFromUser(USER_EMAIL);
    }

    @Test
    @DisplayName("내 포트폴리오 프로필 저장 시 공개 여부, intro, 기술스택을 함께 갱신")
    void updateMyProfile_updateProfileAndTechStacks_success() {
        PortfolioProfileUpdateRequest request = new PortfolioProfileUpdateRequest();
        request.setDisplayName(" 지원 홍길동 ");
        request.setJobCategory(JobCategory.IT);
        request.setIsPortfolioPublic(true);
        request.setIntro(" 백엔드 개발자 ");
        request.setDescription(" 상세 소개 ");
        request.setTechStacks(List.of(" Java ", "", "Spring Boot", "Java"));

        PortfolioProfileDTO before = profile(false, "");
        PortfolioProfileDTO after = profile(true, "백엔드 개발자");
        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(before, after);
        given(portfolioProfileMapper.findTechStacks(USER_EMAIL)).willReturn(List.of("Java", "Spring Boot"));

        PortfolioProfileDTO result = portfolioService.updateMyProfile(USER_EMAIL, request);

        assertThat(result.getIsPortfolioPublic()).isTrue();
        assertThat(result.getIntro()).isEqualTo("백엔드 개발자");
        assertThat(result.getTechStacks()).containsExactly("Java", "Spring Boot");
        verify(portfolioProfileMapper).updateProfile(argThat(profile ->
                USER_EMAIL.equals(profile.getFreelancerEmail())
                        && "지원 홍길동".equals(profile.getDisplayName())
                        && "상세 소개".equals(profile.getDescription())
                        && Boolean.TRUE.equals(profile.getIsPortfolioPublic())
                        && "백엔드 개발자".equals(profile.getIntro())));
        verify(portfolioProfileMapper).deleteTechStacks(USER_EMAIL);
        verify(portfolioProfileMapper).insertTechStack(USER_EMAIL, "Java");
        verify(portfolioProfileMapper).insertTechStack(USER_EMAIL, "Spring Boot");
    }

    @Test
    @DisplayName("포트폴리오 프로필 사진 교체 후 이전 파일 정리를 위임")
    void updateMyProfile_replaceImage_cleanupOldFile() {
        PortfolioProfileUpdateRequest request = new PortfolioProfileUpdateRequest();
        request.setDisplayName("홍길동");
        request.setJobCategory(JobCategory.IT);
        request.setProfileFileId(20);

        PortfolioProfileDTO before = profile(false, "");
        before.setProfileFileId(10);
        PortfolioProfileDTO after = profile(false, "");
        after.setProfileFileId(20);
        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(before, after);
        given(portfolioProfileMapper.findTechStacks(USER_EMAIL)).willReturn(List.of());

        portfolioService.updateMyProfile(USER_EMAIL, request);

        verify(fileService).promoteOwned(20, FileParentType.PORTFOLIO_PROFILE, USER_EMAIL);
        verify(fileService).deleteProfileIfUnreferenced(10);
    }

    @Test
    @DisplayName("포트폴리오 프로필 intro가 30자를 초과하면 실패")
    void updateMyProfile_introTooLong_fail() {
        PortfolioProfileUpdateRequest request = new PortfolioProfileUpdateRequest();
        request.setDisplayName("홍길동");
        request.setJobCategory(JobCategory.IT);
        request.setIntro("가".repeat(31));

        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile(false, ""));

        assertCustomException(() -> portfolioService.updateMyProfile(USER_EMAIL, request),
                ErrorCode.PORTFOLIO_PROFILE_INTRO_TOO_LONG);
        verify(portfolioProfileMapper, never()).updateProfile(any());
        verify(portfolioProfileMapper, never()).deleteTechStacks(any());
    }

    @Test
    @DisplayName("포트폴리오 상세 조회 성공")
    void getOne_success() {
        PortfolioDTO portfolio = basePortfolio(1);
        List<FileDTO> files = List.of(file(10));

        given(portfolioMapper.findById(1)).willReturn(portfolio);
        given(fileService.findByParent(FileParentType.PORTFOLIO_FILE, 1)).willReturn(files);

        Map<String, Object> result = portfolioService.getOne(1);

        assertThat(result).containsEntry("portfolio", portfolio);
        assertThat(result).containsEntry("files", files);
    }

    @Test
    @DisplayName("비공개 프로젝트는 소유자가 아닌 사용자의 직접 조회 차단")
    void getOneForViewer_privateOtherUser_forbidden() {
        PortfolioDTO portfolio = basePortfolio(1);
        portfolio.setIsPublic(false);
        given(portfolioMapper.findById(1)).willReturn(portfolio);

        assertCustomException(
                () -> portfolioService.getOneForViewer(1, "other@test.com"),
                ErrorCode.FORBIDDEN);

        verify(fileService, never()).findByParent(any(), anyInt());
    }

    @Test
    @DisplayName("공개 프로젝트는 소유자가 아닌 사용자도 직접 조회 가능")
    void getOneForViewer_public_success() {
        PortfolioDTO portfolio = basePortfolio(1);
        portfolio.setIsPublic(true);
        given(portfolioMapper.findById(1)).willReturn(portfolio);
        given(fileService.findByParent(FileParentType.PORTFOLIO_FILE, 1)).willReturn(List.of());

        Map<String, Object> result = portfolioService.getOneForViewer(1, "other@test.com");

        assertThat(result).containsEntry("portfolio", portfolio);
    }

    @Test
    @DisplayName("포트폴리오 상세 조회 대상 없음 실패")
    void getOne_notFound_fail() {
        given(portfolioMapper.findById(1)).willReturn(null);

        assertCustomException(() -> portfolioService.getOne(1), ErrorCode.PORTFOLIO_NOT_FOUND);
    }

    @Test
    @DisplayName("포트폴리오 등록 성공")
    void create_success() {
        PortfolioDTO request = basePortfolio(null);
        request.setEmail(null);
        request.setTitle("  프로젝트  ");
        request.setSummary("  한줄 소개  ");
        request.setCategory("design");
        request.setIsPublic(null);
        doAnswer(invocation -> {
            invocation.<PortfolioDTO>getArgument(0).setPortfolioId(42);
            return null;
        }).when(portfolioMapper).insert(request);

        Integer portfolioId = portfolioService.create(request, USER_EMAIL);

        assertThat(portfolioId).isEqualTo(42);
        assertThat(request.getEmail()).isEqualTo(USER_EMAIL);
        assertThat(request.getTitle()).isEqualTo("프로젝트");
        assertThat(request.getSummary()).isEqualTo("한줄 소개");
        assertThat(request.getCategory()).isEqualTo("DESIGN");
        assertThat(request.getIsPublic()).isFalse();
        verify(portfolioMapper).insert(request);
    }

    @Test
    @DisplayName("포트폴리오 등록 API가 생성 ID를 반환")
    void createPortfolio_returnsCreatedId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER_EMAIL,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        PortfolioDTO request = basePortfolio(null);
        doAnswer(invocation -> {
            invocation.<PortfolioDTO>getArgument(0).setPortfolioId(42);
            return null;
        }).when(portfolioMapper).insert(request);

        var response = new PortfolioController(portfolioService).createPortfolio(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getPortfolioId()).isEqualTo(42);
    }

    @Test
    @DisplayName("등록 category null이면 WEB_APP 기본값 적용")
    void create_categoryNull_default() {
        PortfolioDTO request = basePortfolio(null);
        request.setCategory(null);

        portfolioService.create(request, USER_EMAIL);

        assertThat(request.getCategory()).isEqualTo("WEB_APP");
        verify(portfolioMapper).insert(request);
    }

    @Test
    @DisplayName("등록 category blank이면 WEB_APP 기본값 적용")
    void create_categoryBlank_default() {
        PortfolioDTO request = basePortfolio(null);
        request.setCategory("   ");

        portfolioService.create(request, USER_EMAIL);

        assertThat(request.getCategory()).isEqualTo("WEB_APP");
        verify(portfolioMapper).insert(request);
    }

    @Test
    @DisplayName("등록 category 허용값이 아니면 실패")
    void create_invalidCategory_fail() {
        PortfolioDTO request = basePortfolio(null);
        request.setCategory("SERVER");

        assertCustomException(() -> portfolioService.create(request, USER_EMAIL),
                ErrorCode.INVALID_PORTFOLIO_CATEGORY);
        verify(portfolioMapper, never()).insert(any(PortfolioDTO.class));
    }

    @Test
    @DisplayName("등록 시작일이 종료일보다 늦으면 실패")
    void create_invalidPeriod_fail() {
        PortfolioDTO request = basePortfolio(null);
        request.setWorkStartAt(LocalDateTime.of(2026, 6, 2, 0, 0));
        request.setWorkEndAt(LocalDateTime.of(2026, 6, 1, 0, 0));

        assertCustomException(() -> portfolioService.create(request, USER_EMAIL),
                ErrorCode.INVALID_PORTFOLIO_PERIOD);
        verify(portfolioMapper, never()).insert(any(PortfolioDTO.class));
    }

    @Test
    @DisplayName("등록 시작일 또는 종료일 한쪽이 null이면 허용")
    void create_oneSidePeriodNull_success() {
        PortfolioDTO nullStartAt = basePortfolio(null);
        nullStartAt.setWorkStartAt(null);
        PortfolioDTO nullEndAt = basePortfolio(null);
        nullEndAt.setWorkEndAt(null);

        portfolioService.create(nullStartAt, USER_EMAIL);
        portfolioService.create(nullEndAt, USER_EMAIL);

        verify(portfolioMapper, times(2)).insert(any(PortfolioDTO.class));
    }

    @Test
    @DisplayName("등록 title null 또는 공백이면 실패")
    void create_invalidTitle_fail() {
        for (String title : new String[] {null, "", "   "}) {
            PortfolioDTO request = basePortfolio(null);
            request.setTitle(title);

            assertCustomException(() -> portfolioService.create(request, USER_EMAIL), ErrorCode.INVALID_INPUT);
        }

        verify(portfolioMapper, never()).insert(any(PortfolioDTO.class));
    }

    @Test
    @DisplayName("등록 summary null 또는 공백이면 실패")
    void create_invalidSummaryBlank_fail() {
        for (String summary : new String[] {null, "", "   "}) {
            PortfolioDTO request = basePortfolio(null);
            request.setSummary(summary);

            assertCustomException(() -> portfolioService.create(request, USER_EMAIL), ErrorCode.INVALID_INPUT);
        }

        verify(portfolioMapper, never()).insert(any(PortfolioDTO.class));
    }

    @Test
    @DisplayName("등록 summary 30자 초과이면 실패")
    void create_summaryTooLong_fail() {
        PortfolioDTO request = basePortfolio(null);
        request.setSummary("1234567890123456789012345678901");

        assertCustomException(() -> portfolioService.create(request, USER_EMAIL), ErrorCode.INVALID_INPUT);
        verify(portfolioMapper, never()).insert(any(PortfolioDTO.class));
    }

    @Test
    @DisplayName("포트폴리오 전체 수정 성공")
    void update_success() {
        PortfolioDTO existing = basePortfolio(1);
        PortfolioDTO request = basePortfolio(null);
        request.setCategory("planning");

        given(portfolioMapper.findById(1)).willReturn(existing);
        given(portfolioMapper.update(1, request)).willReturn(1);

        portfolioService.update(1, request);

        assertThat(request.getCategory()).isEqualTo("PLANNING");
        verify(portfolioMapper).findById(1);
        verify(portfolioMapper).update(1, request);
    }

    @Test
    @DisplayName("포트폴리오 수정 대상 없음 실패")
    void update_notFound_fail() {
        PortfolioDTO request = basePortfolio(null);
        given(portfolioMapper.findById(1)).willReturn(null);

        assertCustomException(() -> portfolioService.update(1, request), ErrorCode.NOT_FOUND);
        verify(portfolioMapper, never()).update(anyInt(), any(PortfolioDTO.class));
    }

    @Test
    @DisplayName("포트폴리오 삭제 성공")
    void delete_success() {
        PortfolioDTO portfolio = basePortfolio(1);
        portfolio.setBannerFileId(10);
        given(portfolioMapper.findById(1)).willReturn(portfolio);
        given(portfolioMapper.softDelete(1)).willReturn(1);

        portfolioService.delete(1);

        verify(fileService).deleteBySystem(10);
        verify(fileService).deleteByParent(FileParentType.PORTFOLIO_FILE, 1);
        verify(portfolioMapper).softDelete(1);
    }

    @Test
    @DisplayName("등록 권한 실패")
    void createPortfolio_forbiddenRole_fail() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER_EMAIL,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_COMPANY"))));
        PortfolioController controller = new PortfolioController(portfolioService);

        assertCustomException(() -> controller.createPortfolio(basePortfolio(null)), ErrorCode.FREELANCER_ONLY);
        verify(portfolioMapper, never()).insert(any(PortfolioDTO.class));
    }

    @Test
    @DisplayName("회사 계정은 내 포트폴리오 프로필 조회 실패")
    void getMyProfile_forbiddenRole_fail() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        USER_EMAIL,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_COMPANY"))));
        PortfolioController controller = new PortfolioController(portfolioService);

        assertCustomException(controller::getMyProfile, ErrorCode.FREELANCER_ONLY);
    }

    private void assertCustomException(ThrowingCallable callable, ErrorCode expectedErrorCode) {
        assertThatThrownBy(callable::call)
                .isInstanceOfSatisfying(CustomException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private PortfolioDTO basePortfolio(Integer portfolioId) {
        return PortfolioDTO.builder()
                .portfolioId(portfolioId)
                .email(USER_EMAIL)
                .category("WEB_APP")
                .title("프로젝트")
                .summary("한줄 소개")
                .content("프로젝트 설명")
                .workStartAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .workEndAt(LocalDateTime.of(2026, 2, 1, 0, 0))
                .isPublic(true)
                .isDeleted(false)
                .createdAt(LocalDateTime.of(2026, 2, 2, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 2, 3, 0, 0))
                .build();
    }

    private FileDTO file(int fileId) {
        return FileDTO.builder()
                .fileId(fileId)
                .userEmail(USER_EMAIL)
                .parentType(FileParentType.PORTFOLIO_FILE)
                .parentId(1)
                .oriName("file-" + fileId + ".png")
                .uploadPath("portfolio/file-" + fileId + ".png")
                .build();
    }

    private PortfolioProfileDTO profile(boolean isPublic, String intro) {
        return PortfolioProfileDTO.builder()
                .freelancerEmail(USER_EMAIL)
                .displayName("홍길동")
                .jobCategory(JobCategory.IT)
                .profileFileId(null)
                .isPortfolioPublic(isPublic)
                .intro(intro)
                .description("")
                .build();
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
