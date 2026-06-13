package com.ssafy.lancit.domain.recruitment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioProfileMapper;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDTO;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDetailResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationPortfolioSummaryResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationRequest;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.PortfolioPermissionMapper;
import com.ssafy.lancit.domain.recruitment.application.service.ApplicationService;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.ApplicationStatus;
import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final String USER_EMAIL = "user@test.com";
    private static final String OTHER_USER_EMAIL = "other@test.com";
    private static final String COMPANY_EMAIL = "company@test.com";
    private static final String OTHER_COMPANY_EMAIL = "other-company@test.com";
    private static final String ROLE_USER = "USER";
    private static final String ROLE_COMPANY = "COMPANY";

    @InjectMocks
    private ApplicationService applicationService;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private PortfolioPermissionMapper portfolioPermissionMapper;

    @Mock
    private PortfolioMapper portfolioMapper;

    @Mock
    private PortfolioProfileMapper portfolioProfileMapper;

    @Mock
    private RecruitmentMapper recruitmentMapper;

    @Test
    @DisplayName("회사가 자기 공고 지원자 목록 조회 성공")
    void getCompanyApplications_success() {
        PageRequest pageRequest = new PageRequest();
        ApplicationDTO application = application(ApplicationStatus.PENDING, null, null);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyList(10, pageRequest)).willReturn(List.of(application));
        given(applicationMapper.countCompanyList(10)).willReturn(1L);
        given(portfolioPermissionMapper.findPortfolioIdsByApplicationId(1)).willReturn(List.of(1));
        given(portfolioMapper.findApplicationSummariesByIds(List.of(1))).willReturn(portfolios(List.of(1)));

        PageResponse<ApplicationDetailResponse> result =
                applicationService.getCompanyApplications(10, COMPANY_EMAIL, ROLE_COMPANY, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getApplicantName()).isEqualTo("홍길동");
        assertThat(result.getContent().get(0).getViewedAt()).isNull();
        verify(applicationMapper, never()).markViewedIfAbsent(1);
    }

    @Test
    @DisplayName("회사가 자기 공고 지원 상세 조회 성공 - viewedAt 기록")
    void getCompanyApplication_markViewed_success() {
        ApplicationDTO beforeView = application(ApplicationStatus.PENDING, null, null);
        ApplicationDTO afterView = application(ApplicationStatus.PENDING, null,
                LocalDateTime.of(2026, 6, 13, 20, 30));
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1)).willReturn(beforeView, afterView);
        given(portfolioPermissionMapper.findPortfolioIdsByApplicationId(1)).willReturn(List.of(1));
        given(portfolioMapper.findApplicationSummariesByIds(List.of(1))).willReturn(portfolios(List.of(1)));
        stubPortfolioProfile();

        ApplicationDetailResponse result =
                applicationService.getCompanyApplication(10, 1, COMPANY_EMAIL, ROLE_COMPANY);

        assertThat(result.getViewedAt()).isEqualTo(LocalDateTime.of(2026, 6, 13, 20, 30));
        assertThat(result.getPortfolioProfile()).isNotNull();
        assertThat(result.getPortfolioProfile().getTechStacks()).containsExactly("Java", "Spring");
        verify(applicationMapper).markViewedIfAbsent(1);
    }

    @Test
    @DisplayName("회사가 이미 열람한 지원 상세 조회 시 viewedAt 덮어쓰지 않음")
    void getCompanyApplication_alreadyViewed_keepValue() {
        LocalDateTime viewedAt = LocalDateTime.of(2026, 6, 13, 20, 0);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1))
                .willReturn(application(ApplicationStatus.PENDING, null, viewedAt));
        given(portfolioPermissionMapper.findPortfolioIdsByApplicationId(1)).willReturn(List.of(1));
        given(portfolioMapper.findApplicationSummariesByIds(List.of(1))).willReturn(portfolios(List.of(1)));
        stubPortfolioProfile();

        ApplicationDetailResponse result =
                applicationService.getCompanyApplication(10, 1, COMPANY_EMAIL, ROLE_COMPANY);

        assertThat(result.getViewedAt()).isEqualTo(viewedAt);
        verify(applicationMapper, never()).markViewedIfAbsent(1);
    }

    @Test
    @DisplayName("다른 회사가 지원자 목록 조회 실패")
    void getCompanyApplications_otherCompany_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());

        assertCustomException(
                () -> applicationService.getCompanyApplications(10, OTHER_COMPANY_EMAIL, ROLE_COMPANY, new PageRequest()),
                ErrorCode.RECRUITMENT_FORBIDDEN);

        verify(applicationMapper, never()).findCompanyList(anyInt(), any());
    }

    @Test
    @DisplayName("다른 회사가 지원 상세 조회 실패")
    void getCompanyApplication_otherCompany_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());

        assertCustomException(
                () -> applicationService.getCompanyApplication(10, 1, OTHER_COMPANY_EMAIL, ROLE_COMPANY),
                ErrorCode.RECRUITMENT_FORBIDDEN);

        verify(applicationMapper, never()).findCompanyDetail(anyInt(), anyInt());
    }

    @Test
    @DisplayName("프리랜서가 회사용 지원자 목록 조회 실패")
    void getCompanyApplications_userRole_fail() {
        assertCustomException(
                () -> applicationService.getCompanyApplications(10, USER_EMAIL, ROLE_USER, new PageRequest()),
                ErrorCode.RECRUITMENT_COMPANY_ONLY);

        verify(recruitmentMapper, never()).findById(10);
    }

    @Test
    @DisplayName("프리랜서가 회사용 지원 상세 조회 실패")
    void getCompanyApplication_userRole_fail() {
        assertCustomException(
                () -> applicationService.getCompanyApplication(10, 1, USER_EMAIL, ROLE_USER),
                ErrorCode.RECRUITMENT_COMPANY_ONLY);

        verify(recruitmentMapper, never()).findById(10);
    }

    @Test
    @DisplayName("존재하지 않는 공고의 지원자 목록 조회 실패")
    void getCompanyApplications_recruitmentNotFound_fail() {
        given(recruitmentMapper.findById(10)).willReturn(null);

        assertCustomException(
                () -> applicationService.getCompanyApplications(10, COMPANY_EMAIL, ROLE_COMPANY, new PageRequest()),
                ErrorCode.RECRUITMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("applicationId가 recruitmentId에 속하지 않으면 상세 조회 실패")
    void getCompanyApplication_mismatch_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 99)).willReturn(null);

        assertCustomException(
                () -> applicationService.getCompanyApplication(10, 99, COMPANY_EMAIL, ROLE_COMPANY),
                ErrorCode.APPLICATION_NOT_FOUND);

        verify(applicationMapper, never()).markViewedIfAbsent(99);
    }

    @Test
    @DisplayName("OPEN 공고 지원 성공")
    void apply_openRecruitment_success() {
        ApplicationRequest request = request("지원 소개", List.of(1, 3, 3));
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.existsByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(false);
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(1, 3))).willReturn(2);
        doAnswer(invocation -> {
            ApplicationDTO dto = invocation.getArgument(0);
            dto.setApplicationId(1);
            return null;
        }).when(applicationMapper).insert(any(ApplicationDTO.class));
        stubDetail(application(ApplicationStatus.PENDING, null, null), List.of(1, 3));

        ApplicationDetailResponse result = applicationService.apply(10, request, USER_EMAIL, ROLE_USER);

        assertThat(result.getApplicationId()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(result.getPortfolios()).hasSize(2);
        verify(applicationMapper).insert(any(ApplicationDTO.class));
        verify(portfolioPermissionMapper).insertAll(1, List.of(1, 3));
    }

    @Test
    @DisplayName("만료된 공고 지원 실패")
    void apply_expiredRecruitment_fail() {
        RecruitmentDTO recruitment = openRecruitment();
        recruitment.setRecruitmentEndAt(LocalDateTime.now().minusDays(1));
        given(recruitmentMapper.findById(10)).willReturn(recruitment);

        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of(1)), USER_EMAIL, ROLE_USER),
                ErrorCode.RECRUITMENT_NOT_OPEN);

        verify(applicationMapper, never()).insert(any(ApplicationDTO.class));
    }

    @Test
    @DisplayName("CLOSED 공고 지원 실패")
    void apply_closedRecruitment_fail() {
        RecruitmentDTO recruitment = openRecruitment();
        recruitment.setStatus(RecruitmentStatus.CLOSED);
        given(recruitmentMapper.findById(10)).willReturn(recruitment);

        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of(1)), USER_EMAIL, ROLE_USER),
                ErrorCode.RECRUITMENT_NOT_OPEN);
    }

    @Test
    @DisplayName("중복 지원 실패")
    void apply_duplicate_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.existsByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(true);

        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of(1)), USER_EMAIL, ROLE_USER),
                ErrorCode.APPLICATION_ALREADY_EXISTS);

        verify(portfolioMapper, never()).countOwnedActiveByIds(any(), any());
    }

    @Test
    @DisplayName("portfolioIds 빈 배열 실패")
    void apply_emptyPortfolioIds_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.existsByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(false);

        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of()), USER_EMAIL, ROLE_USER),
                ErrorCode.INVALID_APPLICATION_PORTFOLIO);
    }

    @Test
    @DisplayName("타인 포트폴리오 선택 실패")
    void apply_otherUserPortfolio_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.existsByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(false);
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(99))).willReturn(0);

        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of(99)), USER_EMAIL, ROLE_USER),
                ErrorCode.INVALID_APPLICATION_PORTFOLIO);
    }

    @Test
    @DisplayName("삭제된 포트폴리오 선택 실패")
    void apply_deletedPortfolio_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.existsByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(false);
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(1, 2))).willReturn(1);

        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of(1, 2)), USER_EMAIL, ROLE_USER),
                ErrorCode.INVALID_APPLICATION_PORTFOLIO);
    }

    @Test
    @DisplayName("회사 계정 지원 실패")
    void apply_companyRole_fail() {
        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of(1)), OTHER_USER_EMAIL, ROLE_COMPANY),
                ErrorCode.FREELANCER_ONLY);

        verify(recruitmentMapper, never()).findById(10);
    }

    @Test
    @DisplayName("내 지원 조회 성공")
    void getMine_success() {
        stubDetail(application(ApplicationStatus.PENDING, null, null), List.of(1));

        ApplicationDetailResponse result = applicationService.getMine(10, USER_EMAIL, ROLE_USER);

        assertThat(result.getRecruitmentId()).isEqualTo(10);
        assertThat(result.getPortfolios()).hasSize(1);
    }

    @Test
    @DisplayName("내 지원 수정 성공")
    void updateMine_success() {
        ApplicationDTO existing = application(ApplicationStatus.PENDING, null, null);
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(existing, existing);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(2, 4))).willReturn(2);
        given(applicationMapper.updateIntro(1, "수정 소개")).willReturn(1);
        given(portfolioPermissionMapper.findPortfolioIdsByApplicationId(1)).willReturn(List.of(2, 4));
        given(portfolioMapper.findApplicationSummariesByIds(List.of(2, 4))).willReturn(portfolios(List.of(2, 4)));

        ApplicationDetailResponse result =
                applicationService.updateMine(10, request(" 수정 소개 ", List.of(2, 4)), USER_EMAIL, ROLE_USER);

        assertThat(result.getPortfolios()).extracting("portfolioId").containsExactly(2, 4);
        verify(portfolioPermissionMapper).deleteByApplicationId(1);
        verify(portfolioPermissionMapper).insertAll(1, List.of(2, 4));
        verify(applicationMapper).updateIntro(1, "수정 소개");
    }

    @Test
    @DisplayName("viewed_at 있는 지원 수정 실패")
    void updateMine_viewed_fail() {
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL))
                .willReturn(application(ApplicationStatus.PENDING, null, LocalDateTime.now()));

        assertCustomException(
                () -> applicationService.updateMine(10, request("수정", List.of(1)), USER_EMAIL, ROLE_USER),
                ErrorCode.APPLICATION_ALREADY_VIEWED);

        verify(applicationMapper, never()).updateIntro(eq(1), any());
    }

    @Test
    @DisplayName("CANCELLED 지원 수정 실패")
    void updateMine_cancelled_fail() {
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL))
                .willReturn(application(ApplicationStatus.CANCELLED, LocalDateTime.now(), null));

        assertCustomException(
                () -> applicationService.updateMine(10, request("수정", List.of(1)), USER_EMAIL, ROLE_USER),
                ErrorCode.INVALID_APPLICATION_STATUS);
    }

    @Test
    @DisplayName("내 지원 취소 성공")
    void cancelMine_success() {
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL))
                .willReturn(application(ApplicationStatus.PENDING, null, null));
        given(applicationMapper.cancel(1)).willReturn(1);

        applicationService.cancelMine(10, USER_EMAIL, ROLE_USER);

        verify(applicationMapper).cancel(1);
        verify(portfolioPermissionMapper, never()).deleteByApplicationId(1);
    }

    @Test
    @DisplayName("viewed_at 있는 지원 취소 실패")
    void cancelMine_viewed_fail() {
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL))
                .willReturn(application(ApplicationStatus.PENDING, null, LocalDateTime.now()));

        assertCustomException(
                () -> applicationService.cancelMine(10, USER_EMAIL, ROLE_USER),
                ErrorCode.APPLICATION_ALREADY_VIEWED);

        verify(applicationMapper, never()).cancel(1);
    }

    @Test
    @DisplayName("이미 취소된 지원 취소 실패")
    void cancelMine_alreadyCancelled_fail() {
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL))
                .willReturn(application(ApplicationStatus.CANCELLED, LocalDateTime.now(), null));

        assertCustomException(
                () -> applicationService.cancelMine(10, USER_EMAIL, ROLE_USER),
                ErrorCode.APPLICATION_ALREADY_CANCELLED);
    }

    private void stubDetail(ApplicationDTO application, List<Integer> portfolioIds) {
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(application);
        given(portfolioPermissionMapper.findPortfolioIdsByApplicationId(application.getApplicationId()))
                .willReturn(portfolioIds);
        given(portfolioMapper.findApplicationSummariesByIds(portfolioIds)).willReturn(portfolios(portfolioIds));
    }

    private void stubPortfolioProfile() {
        given(portfolioProfileMapper.findByFreelancerEmail(USER_EMAIL)).willReturn(PortfolioProfileDTO.builder()
                .freelancerEmail(USER_EMAIL)
                .name("홍길동")
                .jobCategory(JobCategory.IT)
                .profileFileId(11)
                .isPortfolioPublic(false)
                .intro("백엔드 개발자")
                .build());
        given(portfolioProfileMapper.findTechStacks(USER_EMAIL)).willReturn(List.of("Java", "Spring"));
    }

    private ApplicationRequest request(String intro, List<Integer> portfolioIds) {
        ApplicationRequest request = new ApplicationRequest();
        request.setIntro(intro);
        request.setPortfolioIds(portfolioIds);
        return request;
    }

    private RecruitmentDTO openRecruitment() {
        return RecruitmentDTO.builder()
                .recruitmentId(10)
                .companyEmail(COMPANY_EMAIL)
                .title("백엔드 개발자 모집")
                .summary("요약")
                .content("내용")
                .jobCategory(JobCategory.IT)
                .recruitmentCategory(RecruitmentCategory.WEB_APP)
                .status(RecruitmentStatus.OPEN)
                .recruitmentEndAt(LocalDateTime.now().plusDays(1))
                .build();
    }

    private ApplicationDTO application(ApplicationStatus status,
                                       LocalDateTime canceledAt,
                                       LocalDateTime viewedAt) {
        return ApplicationDTO.builder()
                .applicationId(1)
                .recruitmentId(10)
                .recruitmentTitle("백엔드 개발자 모집")
                .applicantEmail(USER_EMAIL)
                .applicantName("홍길동")
                .intro("지원 소개")
                .status(status)
                .appliedAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .canceledAt(canceledAt)
                .viewedAt(viewedAt)
                .build();
    }

    private List<ApplicationPortfolioSummaryResponse> portfolios(List<Integer> portfolioIds) {
        return portfolioIds.stream()
                .map(portfolioId -> ApplicationPortfolioSummaryResponse.builder()
                        .portfolioId(portfolioId)
                        .title("프로젝트 " + portfolioId)
                        .summary("요약")
                        .category("WEB_APP")
                        .bannerFileId(10)
                        .isPublic(false)
                        .build())
                .toList();
    }

    private void assertCustomException(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }
}
