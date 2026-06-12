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

        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile(true));
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

        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile(true));
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
    @DisplayName("공개 프로필이 아니면 공개 포트폴리오 목록 조회 실패")
    void getPublicList_privateProfile_fail() {
        PageRequest pageRequest = new PageRequest();
        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile(false));

        assertCustomException(() -> portfolioService.getPublicList(USER_EMAIL, pageRequest),
                ErrorCode.PORTFOLIO_PROFILE_NOT_PUBLIC);
        verify(portfolioMapper, never()).findPublicByEmail(any(), any(), any());
    }

    @Test
    @DisplayName("내 포트폴리오 프로필 기본값 조회 성공")
    void getMyProfile_default_success() {
        PortfolioProfileDTO profile = profile(false);
        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile);
        given(portfolioProfileMapper.findTechStacks(USER_EMAIL)).willReturn(List.of());

        PortfolioProfileDTO result = portfolioService.getMyProfile(USER_EMAIL);

        assertThat(result.getFreelancerEmail()).isEqualTo(USER_EMAIL);
        assertThat(result.getIsPortfolioPublic()).isFalse();
        assertThat(result.getTechStacks()).isEmpty();
    }

    @Test
    @DisplayName("포트폴리오 프로필 저장 시 row가 없으면 생성하고 기술스택을 정규화한다")
    void updateMyProfile_insertAndNormalizeTechStacks_success() {
        PortfolioProfileUpdateRequest request = new PortfolioProfileUpdateRequest();
        request.setIsPortfolioPublic(true);
        request.setShortIntro(" 백엔드 개발자 ");
        request.setDescription(" 안정적인 API 설계를 좋아합니다. ");
        request.setTechStacks(List.of(" java ", "", "Spring Boot", "java", "mysql"));

        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile(false));
        given(portfolioProfileMapper.existsProfile(USER_EMAIL)).willReturn(false);
        given(portfolioProfileMapper.findTechStacks(USER_EMAIL))
                .willReturn(List.of("JAVA", "MYSQL", "SPRING BOOT"));

        PortfolioProfileDTO result = portfolioService.updateMyProfile(USER_EMAIL, request);

        assertThat(result.getTechStacks()).containsExactly("JAVA", "MYSQL", "SPRING BOOT");
        verify(portfolioProfileMapper).insertProfile(argThat(profile ->
                "백엔드 개발자".equals(profile.getShortIntro())
                        && "안정적인 API 설계를 좋아합니다.".equals(profile.getDescription())
                        && Boolean.TRUE.equals(profile.getIsPortfolioPublic())));
        verify(portfolioProfileMapper).deleteTechStacks(USER_EMAIL);
        verify(portfolioProfileMapper).insertTechStack(USER_EMAIL, "JAVA");
        verify(portfolioProfileMapper).insertTechStack(USER_EMAIL, "SPRING BOOT");
        verify(portfolioProfileMapper).insertTechStack(USER_EMAIL, "MYSQL");
    }

    @Test
    @DisplayName("포트폴리오 한 줄 소개가 없으면 실패")
    void updateMyProfile_shortIntroRequired_fail() {
        PortfolioProfileUpdateRequest request = new PortfolioProfileUpdateRequest();
        request.setShortIntro("   ");

        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile(false));

        assertCustomException(() -> portfolioService.updateMyProfile(USER_EMAIL, request),
                ErrorCode.PORTFOLIO_PROFILE_SHORT_INTRO_REQUIRED);
        verify(portfolioProfileMapper, never()).insertProfile(any());
        verify(portfolioProfileMapper, never()).updateProfile(any());
    }

    @Test
    @DisplayName("포트폴리오 한 줄 소개가 30자를 초과하면 실패")
    void updateMyProfile_shortIntroTooLong_fail() {
        PortfolioProfileUpdateRequest request = new PortfolioProfileUpdateRequest();
        request.setShortIntro("가".repeat(31));

        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile(false));

        assertCustomException(() -> portfolioService.updateMyProfile(USER_EMAIL, request),
                ErrorCode.PORTFOLIO_PROFILE_SHORT_INTRO_TOO_LONG);
        verify(portfolioProfileMapper, never()).insertProfile(any());
        verify(portfolioProfileMapper, never()).updateProfile(any());
    }

    @Test
    @DisplayName("포트폴리오 소개글은 null을 허용하고 blank는 null로 저장")
    void updateMyProfile_descriptionBlank_savedNull_success() {
        PortfolioProfileUpdateRequest request = new PortfolioProfileUpdateRequest();
        request.setShortIntro("백엔드 개발자");
        request.setDescription("   ");

        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile(false));
        given(portfolioProfileMapper.existsProfile(USER_EMAIL)).willReturn(false);
        given(portfolioProfileMapper.findTechStacks(USER_EMAIL)).willReturn(List.of());

        portfolioService.updateMyProfile(USER_EMAIL, request);

        verify(portfolioProfileMapper).insertProfile(argThat(profile ->
                "백엔드 개발자".equals(profile.getShortIntro())
                        && profile.getDescription() == null));
    }

    @Test
    @DisplayName("포트폴리오 소개글이 200자를 초과하면 실패")
    void updateMyProfile_descriptionTooLong_fail() {
        PortfolioProfileUpdateRequest request = new PortfolioProfileUpdateRequest();
        request.setShortIntro("백엔드 개발자");
        request.setDescription("a".repeat(201));

        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(profile(false));

        assertCustomException(() -> portfolioService.updateMyProfile(USER_EMAIL, request),
                ErrorCode.PORTFOLIO_PROFILE_DESCRIPTION_TOO_LONG);
        verify(portfolioProfileMapper, never()).insertProfile(any());
        verify(portfolioProfileMapper, never()).updateProfile(any());
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

        portfolioService.create(request, USER_EMAIL);

        assertThat(request.getEmail()).isEqualTo(USER_EMAIL);
        assertThat(request.getTitle()).isEqualTo("프로젝트");
        assertThat(request.getSummary()).isEqualTo("한줄 소개");
        assertThat(request.getCategory()).isEqualTo("DESIGN");
        assertThat(request.getIsPublic()).isFalse();
        verify(portfolioMapper).insert(request);
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
        given(portfolioMapper.softDelete(1)).willReturn(1);

        portfolioService.delete(1);

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

    private PortfolioProfileDTO profile(boolean isPublic) {
        return PortfolioProfileDTO.builder()
                .freelancerEmail(USER_EMAIL)
                .name("홍길동")
                .jobCategory(JobCategory.IT)
                .isPortfolioPublic(isPublic)
                .shortIntro("백엔드 개발자")
                .description("안정적인 API 설계를 좋아합니다.")
                .viewCount(0)
                .build();
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call();
    }
}
