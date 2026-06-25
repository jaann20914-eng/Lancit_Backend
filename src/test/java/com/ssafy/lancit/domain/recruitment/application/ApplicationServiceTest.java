package com.ssafy.lancit.domain.recruitment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.portfolio.service.PortfolioService;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDTO;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDetailResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationPortfolioSummaryResponse;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationProfileSnapshotDTO;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationProfileSnapshotRequest;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationRequest;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationStatusUpdateRequest;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationPortfolioSnapshotMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationProfileSnapshotMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.PortfolioPermissionMapper;
import com.ssafy.lancit.domain.recruitment.application.service.ApplicationService;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.ApplicationStatus;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.FileParentType;
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
    private PortfolioService portfolioService;

    @Mock
    private ApplicationProfileSnapshotMapper applicationProfileSnapshotMapper;

    @Mock
    private ApplicationPortfolioSnapshotMapper applicationPortfolioSnapshotMapper;

    @Mock
    private FileService fileService;

    @Mock
    private RecruitmentMapper recruitmentMapper;

    @Mock
    private ContractMapper contractMapper;

    @Test
    @DisplayName("회사가 자기 공고 지원자 목록 조회 성공")
    void getCompanyApplications_success() {
        PageRequest pageRequest = new PageRequest();
        ApplicationDTO application = application(ApplicationStatus.PENDING, null, null);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyList(10, pageRequest)).willReturn(List.of(application));
        given(applicationMapper.countCompanyList(10)).willReturn(1L);
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(1))
                .willReturn(portfolios(List.of(1)));

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
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(1))
                .willReturn(portfolios(List.of(1)));
        stubPortfolioProfile();

        ApplicationDetailResponse result =
                applicationService.getCompanyApplication(10, 1, COMPANY_EMAIL, ROLE_COMPANY);

        assertThat(result.getViewedAt()).isEqualTo(LocalDateTime.of(2026, 6, 13, 20, 30));
        assertThat(result.getPortfolioProfile()).isNotNull();
        assertThat(result.getApplicantName()).isEqualTo("지원 홍길동");
        assertThat(result.getPortfolioProfile().getTechStacks()).containsExactly("Java", "Spring");
        verify(applicationMapper).markViewedIfAbsent(1);
        verify(portfolioService, never()).getMyProfile(any());
    }

    @Test
    @DisplayName("공고 작성 회사가 지원서에 선택된 비공개 프로젝트 상세 조회 성공")
    void getCompanyApplicationPortfolio_permitted_success() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1))
                .willReturn(application(ApplicationStatus.PENDING, null, null));
        given(portfolioPermissionMapper.existsCompanyPermission(1, 3, 10, COMPANY_EMAIL))
                .willReturn(true);
        PortfolioDTO snapshot = PortfolioDTO.builder().portfolioId(3).title("지원 당시 프로젝트").build();
        given(applicationPortfolioSnapshotMapper.findPortfolio(1, 3)).willReturn(snapshot);
        given(applicationPortfolioSnapshotMapper.findFiles(1, 3)).willReturn(List.of());

        Map<String, Object> result = applicationService.getCompanyApplicationPortfolio(
                10, 1, 3, COMPANY_EMAIL, ROLE_COMPANY);

        assertThat(result).containsEntry("portfolio", snapshot);
        verify(portfolioService, never()).getOne(3);
    }

    @Test
    @DisplayName("선택하지 않은 프로젝트는 공고 작성 회사도 상세 조회 실패")
    void getCompanyApplicationPortfolio_notSelected_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1))
                .willReturn(application(ApplicationStatus.PENDING, null, null));
        given(portfolioPermissionMapper.existsCompanyPermission(1, 99, 10, COMPANY_EMAIL))
                .willReturn(false);

        assertCustomException(
                () -> applicationService.getCompanyApplicationPortfolio(
                        10, 1, 99, COMPANY_EMAIL, ROLE_COMPANY),
                ErrorCode.FORBIDDEN);

        verify(portfolioService, never()).getOne(99);
    }

    @Test
    @DisplayName("회사가 이미 열람한 지원 상세 조회 시 viewedAt 덮어쓰지 않음")
    void getCompanyApplication_alreadyViewed_keepValue() {
        LocalDateTime viewedAt = LocalDateTime.of(2026, 6, 13, 20, 0);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1))
                .willReturn(application(ApplicationStatus.PENDING, null, viewedAt));
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(1))
                .willReturn(portfolios(List.of(1)));
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
        ApplicationDTO saved = application(ApplicationStatus.PENDING, null, null);
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(null, saved);
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(1, 3))).willReturn(2);
        doAnswer(invocation -> {
            ApplicationDTO dto = invocation.getArgument(0);
            dto.setApplicationId(1);
            return null;
        }).when(applicationMapper).insert(any(ApplicationDTO.class));
        stubCurrentPortfolioProfile();
        stubPortfolioProfile();
        stubPortfolioSnapshotWrites(List.of(1, 3));
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(1))
                .willReturn(portfolios(List.of(1, 3)));

        ApplicationDetailResponse result = applicationService.apply(10, request, USER_EMAIL, ROLE_USER);

        assertThat(result.getApplicationId()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(result.getPortfolios()).hasSize(2);
        verify(applicationMapper).insert(any(ApplicationDTO.class));
        verify(portfolioPermissionMapper).insertAll(1, List.of(1, 3));
        verify(applicationPortfolioSnapshotMapper).insertPortfolio(1, 1, 0);
        verify(applicationPortfolioSnapshotMapper).insertPortfolio(1, 3, 1);
        verify(applicationPortfolioSnapshotMapper).insertFiles(1, 1);
        verify(applicationPortfolioSnapshotMapper).insertFiles(1, 3);
        verify(applicationProfileSnapshotMapper).insert(any(ApplicationProfileSnapshotDTO.class));
        verify(applicationProfileSnapshotMapper).insertTechStack(1, "Java", 0);
        verify(applicationProfileSnapshotMapper).insertTechStack(1, "Spring", 1);
    }

    @Test
    @DisplayName("지원용 프로필 카드가 있으면 원본 포트폴리오 프로필 대신 요청값으로 스냅샷 생성")
    void apply_withProfileSnapshotRequest_usesRequestProfileOnly() {
        ApplicationProfileSnapshotRequest profileRequest =
                profileRequest(" 지원 전용 홍길동 ", 20, List.of(" Spring ", "React", "Spring"));
        ApplicationRequest request = request("지원 소개", List.of(1), profileRequest);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        ApplicationDTO saved = application(ApplicationStatus.PENDING, null, null);
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(null, saved);
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(1))).willReturn(1);
        doAnswer(invocation -> {
            ApplicationDTO dto = invocation.getArgument(0);
            dto.setApplicationId(1);
            return null;
        }).when(applicationMapper).insert(any(ApplicationDTO.class));
        given(fileService.findById(20)).willReturn(FileDTO.builder()
                .fileId(20)
                .userEmail(USER_EMAIL)
                .parentType(FileParentType.TEMP)
                .build());
        stubPortfolioSnapshotWrites(List.of(1));
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(1))
                .willReturn(portfolios(List.of(1)));
        given(applicationProfileSnapshotMapper.findByApplicationId(1))
                .willReturn(ApplicationProfileSnapshotDTO.builder()
                        .applicationId(1)
                        .displayName("지원 전용 홍길동")
                        .jobCategory(JobCategory.IT)
                        .profileFileId(20)
                        .intro("백엔드 개발자")
                        .description("지원 전용 상세 소개")
                        .isPortfolioPublic(false)
                        .build());
        given(applicationProfileSnapshotMapper.findTechStacksByApplicationId(1))
                .willReturn(List.of("Spring", "React"));

        ApplicationDetailResponse result = applicationService.apply(10, request, USER_EMAIL, ROLE_USER);

        assertThat(result.getPortfolioProfile().getDisplayName()).isEqualTo("지원 전용 홍길동");
        assertThat(result.getPortfolioProfile().getProfileFileId()).isEqualTo(20);
        assertThat(result.getPortfolioProfile().getTechStacks()).containsExactly("Spring", "React");
        verify(fileService).promoteOwned(20, FileParentType.PORTFOLIO_PROFILE, USER_EMAIL);
        verify(portfolioService, never()).getMyProfile(any());
        verify(applicationProfileSnapshotMapper).insert(argThat(snapshot ->
                snapshot.getApplicationId().equals(1)
                        && "지원 전용 홍길동".equals(snapshot.getDisplayName())
                        && snapshot.getProfileFileId().equals(20)
                        && "백엔드 개발자".equals(snapshot.getIntro())
                        && "지원 전용 상세 소개".equals(snapshot.getDescription())));
        verify(applicationProfileSnapshotMapper).insertTechStack(1, "Spring", 0);
        verify(applicationProfileSnapshotMapper).insertTechStack(1, "React", 1);
    }

    @Test
    @DisplayName("포트폴리오 스냅샷이 없는 레거시 지원도 회사 목록에서 빈 목록으로 조회")
    void getCompanyApplications_withoutPortfolioSnapshot_returnsEmptyPortfolios() {
        PageRequest pageRequest = new PageRequest();
        ApplicationDTO application = application(ApplicationStatus.PENDING, null, null);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyList(10, pageRequest)).willReturn(List.of(application));
        given(applicationMapper.countCompanyList(10)).willReturn(1L);
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(1)).willReturn(null);

        PageResponse<ApplicationDetailResponse> result =
                applicationService.getCompanyApplications(10, COMPANY_EMAIL, ROLE_COMPANY, pageRequest);

        assertThat(result.getContent()).singleElement()
                .extracting(ApplicationDetailResponse::getPortfolios)
                .isEqualTo(List.of());
    }

    @Test
    @DisplayName("취소한 지원서는 같은 ID로 재활성화하고 제출 스냅샷을 교체")
    void apply_cancelledApplication_reactivatesAndReplacesSnapshots() {
        ApplicationDTO cancelled = application(
                ApplicationStatus.CANCELLED,
                LocalDateTime.of(2026, 6, 10, 0, 0),
                LocalDateTime.of(2026, 6, 9, 0, 0));
        cancelled.setContractId(77);
        ApplicationDTO reactivated = application(ApplicationStatus.PENDING, null, null);

        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL))
                .willReturn(cancelled, reactivated);
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(2, 4))).willReturn(2);
        given(applicationPortfolioSnapshotMapper.findFileIdsByApplicationId(1)).willReturn(List.of(90, 91));
        given(applicationMapper.reactivateCancelled(1, "재지원 소개")).willReturn(1);
        stubCurrentPortfolioProfile();
        stubPortfolioProfile();
        stubPortfolioSnapshotWrites(List.of(2, 4));
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(1))
                .willReturn(portfolios(List.of(2, 4)));

        ApplicationDetailResponse result = applicationService.apply(
                10, request(" 재지원 소개 ", List.of(2, 4)), USER_EMAIL, ROLE_USER);

        assertThat(result.getApplicationId()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(result.getCanceledAt()).isNull();
        assertThat(result.getViewedAt()).isNull();
        assertThat(result.getContractId()).isNull();
        assertThat(result.getPortfolios()).extracting("portfolioId").containsExactly(2, 4);
        verify(applicationMapper).reactivateCancelled(1, "재지원 소개");
        verify(portfolioPermissionMapper).deleteByApplicationId(1);
        verify(applicationPortfolioSnapshotMapper).deleteByApplicationId(1);
        verify(applicationProfileSnapshotMapper).deleteTechStacksByApplicationId(1);
        verify(applicationProfileSnapshotMapper).deleteByApplicationId(1);
        verify(portfolioPermissionMapper).insertAll(1, List.of(2, 4));
        verify(fileService).deletePortfolioFileIfUnreferenced(90);
        verify(fileService).deletePortfolioFileIfUnreferenced(91);
        verify(applicationMapper, never()).insert(any(ApplicationDTO.class));
    }

    @Test
    @DisplayName("취소 지원서 재활성화 경쟁에서 상태가 먼저 바뀌면 중복 지원 처리")
    void apply_cancelledApplication_concurrentReactivation_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL))
                .willReturn(application(ApplicationStatus.CANCELLED, LocalDateTime.now(), null));
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(1))).willReturn(1);
        given(applicationPortfolioSnapshotMapper.findFileIdsByApplicationId(1)).willReturn(List.of());
        given(applicationMapper.reactivateCancelled(1, "재지원")).willReturn(0);

        assertCustomException(
                () -> applicationService.apply(
                        10, request("재지원", List.of(1)), USER_EMAIL, ROLE_USER),
                ErrorCode.APPLICATION_ALREADY_EXISTS);

        verify(portfolioPermissionMapper, never()).deleteByApplicationId(1);
        verify(applicationProfileSnapshotMapper, never()).deleteByApplicationId(1);
    }

    @Test
    @DisplayName("프로필 스냅샷 저장 실패를 전파해 지원 트랜잭션 롤백")
    void apply_profileSnapshotFailure_propagates() {
        ApplicationRequest request = request("지원 소개", List.of(1));
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(null);
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(1))).willReturn(1);
        doAnswer(invocation -> {
            ApplicationDTO dto = invocation.getArgument(0);
            dto.setApplicationId(1);
            return null;
        }).when(applicationMapper).insert(any(ApplicationDTO.class));
        stubCurrentPortfolioProfile();
        stubPortfolioSnapshotWrites(List.of(1));
        doThrow(new IllegalStateException("snapshot insert failed"))
                .when(applicationProfileSnapshotMapper).insert(any(ApplicationProfileSnapshotDTO.class));

        assertThatThrownBy(() -> applicationService.apply(10, request, USER_EMAIL, ROLE_USER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("snapshot insert failed");

        verify(applicationMapper).insert(any(ApplicationDTO.class));
        verify(portfolioPermissionMapper).insertAll(1, List.of(1));
    }

    @Test
    @DisplayName("다른 회사는 선택된 프로젝트 상세도 조회 실패")
    void getCompanyApplicationPortfolio_otherCompany_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());

        assertCustomException(
                () -> applicationService.getCompanyApplicationPortfolio(
                        10, 1, 3, OTHER_COMPANY_EMAIL, ROLE_COMPANY),
                ErrorCode.RECRUITMENT_FORBIDDEN);

        verify(portfolioPermissionMapper, never())
                .existsCompanyPermission(anyInt(), anyInt(), anyInt(), any());
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
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL))
                .willReturn(application(ApplicationStatus.PENDING, null, null));

        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of(1)), USER_EMAIL, ROLE_USER),
                ErrorCode.APPLICATION_ALREADY_EXISTS);

        verify(portfolioMapper, never()).countOwnedActiveByIds(any(), any());
    }

    @Test
    @DisplayName("portfolioIds 빈 배열 실패")
    void apply_emptyPortfolioIds_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());

        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of()), USER_EMAIL, ROLE_USER),
                ErrorCode.INVALID_APPLICATION_PORTFOLIO);
    }

    @Test
    @DisplayName("타인 포트폴리오 선택 실패")
    void apply_otherUserPortfolio_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(99))).willReturn(0);

        assertCustomException(
                () -> applicationService.apply(10, request("지원", List.of(99)), USER_EMAIL, ROLE_USER),
                ErrorCode.INVALID_APPLICATION_PORTFOLIO);
    }

    @Test
    @DisplayName("삭제된 포트폴리오 선택 실패")
    void apply_deletedPortfolio_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
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
        stubPortfolioProfile();

        ApplicationDetailResponse result = applicationService.getMine(10, USER_EMAIL, ROLE_USER);

        assertThat(result.getRecruitmentId()).isEqualTo(10);
        assertThat(result.getPortfolios()).hasSize(1);
        assertThat(result.getPortfolioProfile()).isNotNull();
        assertThat(result.getPortfolioProfile().getDisplayName()).isEqualTo("지원 홍길동");
        assertThat(result.getPortfolioProfile().getTechStacks()).containsExactly("Java", "Spring");
    }

    @Test
    @DisplayName("내 지원 수정 성공")
    void updateMine_success() {
        ApplicationDTO existing = application(ApplicationStatus.PENDING, null, null);
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(existing, existing);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(2, 4))).willReturn(2);
        given(applicationMapper.updateIntro(1, "수정 소개")).willReturn(1);
        given(applicationPortfolioSnapshotMapper.findFileIdsByApplicationId(1)).willReturn(List.of(90));
        stubPortfolioSnapshotWrites(List.of(2, 4));
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(1))
                .willReturn(portfolios(List.of(2, 4)));

        ApplicationDetailResponse result =
                applicationService.updateMine(10, request(" 수정 소개 ", List.of(2, 4)), USER_EMAIL, ROLE_USER);

        assertThat(result.getPortfolios()).extracting("portfolioId").containsExactly(2, 4);
        verify(portfolioPermissionMapper).deleteByApplicationId(1);
        verify(portfolioPermissionMapper).insertAll(1, List.of(2, 4));
        verify(applicationPortfolioSnapshotMapper).deleteByApplicationId(1);
        verify(fileService).deletePortfolioFileIfUnreferenced(90);
        verify(applicationMapper).updateIntro(1, "수정 소개");
    }

    @Test
    @DisplayName("내 지원 수정 시 지원용 프로필 스냅샷도 요청값으로 교체")
    void updateMine_withProfileSnapshotRequest_replacesProfileSnapshot() {
        ApplicationDTO existing = application(ApplicationStatus.PENDING, null, null);
        ApplicationProfileSnapshotRequest profileRequest =
                profileRequest("수정 지원자", 20, List.of("Kotlin"));
        ApplicationProfileSnapshotDTO previousProfile = ApplicationProfileSnapshotDTO.builder()
                .applicationId(1)
                .displayName("기존 지원자")
                .jobCategory(JobCategory.IT)
                .profileFileId(10)
                .intro("기존")
                .description("기존 상세")
                .build();
        ApplicationProfileSnapshotDTO updatedProfile = ApplicationProfileSnapshotDTO.builder()
                .applicationId(1)
                .displayName("수정 지원자")
                .jobCategory(JobCategory.IT)
                .profileFileId(20)
                .intro("백엔드 개발자")
                .description("지원 전용 상세 소개")
                .build();
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(existing, existing);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(portfolioMapper.countOwnedActiveByIds(USER_EMAIL, List.of(2, 4))).willReturn(2);
        given(applicationMapper.updateIntro(1, "수정 소개")).willReturn(1);
        given(applicationPortfolioSnapshotMapper.findFileIdsByApplicationId(1)).willReturn(List.of(90));
        given(fileService.findById(20)).willReturn(FileDTO.builder()
                .fileId(20)
                .userEmail(USER_EMAIL)
                .parentType(FileParentType.PORTFOLIO_PROFILE)
                .build());
        given(applicationProfileSnapshotMapper.findByApplicationId(1))
                .willReturn(previousProfile, updatedProfile);
        given(applicationProfileSnapshotMapper.findTechStacksByApplicationId(1))
                .willReturn(List.of("Kotlin"));
        stubPortfolioSnapshotWrites(List.of(2, 4));
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(1))
                .willReturn(portfolios(List.of(2, 4)));

        ApplicationDetailResponse result = applicationService.updateMine(
                10, request(" 수정 소개 ", List.of(2, 4), profileRequest), USER_EMAIL, ROLE_USER);

        assertThat(result.getPortfolioProfile().getDisplayName()).isEqualTo("수정 지원자");
        assertThat(result.getPortfolioProfile().getProfileFileId()).isEqualTo(20);
        assertThat(result.getPortfolioProfile().getTechStacks()).containsExactly("Kotlin");
        verify(applicationProfileSnapshotMapper).deleteTechStacksByApplicationId(1);
        verify(applicationProfileSnapshotMapper).deleteByApplicationId(1);
        verify(applicationProfileSnapshotMapper).insert(argThat(snapshot ->
                "수정 지원자".equals(snapshot.getDisplayName())
                        && snapshot.getProfileFileId().equals(20)));
        verify(applicationProfileSnapshotMapper).insertTechStack(1, "Kotlin", 0);
        verify(fileService).deleteProfileIfUnreferenced(10);
        verify(fileService, never()).promoteOwned(eq(20), eq(FileParentType.PORTFOLIO_PROFILE), eq(USER_EMAIL));
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

    @Test
    @DisplayName("공고 작성 회사가 PENDING 지원을 수락하면 WAITING 계약을 생성하고 연결한다")
    void updateStatus_accept_success() {
        ApplicationDTO pending = application(ApplicationStatus.PENDING, null, null);
        ApplicationDTO accepted = application(ApplicationStatus.ACCEPTED, null, null);
        accepted.setContractId(7);
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1)).willReturn(pending, accepted);
        given(contractMapper.existsActiveContract(10, USER_EMAIL)).willReturn(false);
        given(recruitmentMapper.closeIfOpen(10)).willReturn(1);
        given(applicationMapper.updateStatusIfPending(1, ApplicationStatus.ACCEPTED)).willReturn(1);
        doAnswer(invocation -> {
            ContractDTO contract = invocation.getArgument(0);
            assertThat(contract.getStatus()).isEqualTo(ContractStatus.WAITING);
            contract.setContractId(7);
            return 1;
        }).when(contractMapper).insert(any(ContractDTO.class));
        given(applicationMapper.attachContract(1, 7)).willReturn(1);

        ApplicationDetailResponse result = applicationService.updateStatus(
                10, 1, statusRequest("ACCEPTED"), COMPANY_EMAIL, ROLE_COMPANY);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(result.getContractId()).isEqualTo(7);
        verify(contractMapper).insert(any(ContractDTO.class));
        verify(applicationMapper).attachContract(1, 7);
        verify(recruitmentMapper).closeIfOpen(10);
    }

    @Test
    @DisplayName("이미 마감된 공고의 지원자는 채택할 수 없다")
    void updateStatus_acceptClosedRecruitment_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1))
                .willReturn(application(ApplicationStatus.PENDING, null, null));
        given(contractMapper.existsActiveContract(10, USER_EMAIL)).willReturn(false);
        given(recruitmentMapper.closeIfOpen(10)).willReturn(0);

        assertCustomException(
                () -> applicationService.updateStatus(
                        10, 1, statusRequest("ACCEPTED"), COMPANY_EMAIL, ROLE_COMPANY),
                ErrorCode.RECRUITMENT_NOT_OPEN);

        verify(applicationMapper, never()).updateStatusIfPending(anyInt(), any());
        verify(contractMapper, never()).insert(any());
    }

    @Test
    @DisplayName("공고 작성 회사가 PENDING 지원을 거절하면 계약을 생성하지 않는다")
    void updateStatus_reject_success() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1)).willReturn(
                application(ApplicationStatus.PENDING, null, null),
                application(ApplicationStatus.REJECTED, null, null));
        given(applicationMapper.updateStatusIfPending(1, ApplicationStatus.REJECTED)).willReturn(1);

        ApplicationDetailResponse result = applicationService.updateStatus(
                10, 1, statusRequest("REJECTED"), COMPANY_EMAIL, ROLE_COMPANY);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(contractMapper, never()).insert(any());
    }

    @Test
    @DisplayName("다른 회사는 지원 상태를 변경할 수 없다")
    void updateStatus_otherCompany_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());

        assertCustomException(
                () -> applicationService.updateStatus(
                        10, 1, statusRequest("ACCEPTED"), OTHER_COMPANY_EMAIL, ROLE_COMPANY),
                ErrorCode.RECRUITMENT_FORBIDDEN);

        verify(applicationMapper, never()).updateStatusIfPending(anyInt(), any());
    }

    @Test
    @DisplayName("프리랜서는 지원 상태를 변경할 수 없다")
    void updateStatus_userRole_fail() {
        assertCustomException(
                () -> applicationService.updateStatus(
                        10, 1, statusRequest("ACCEPTED"), USER_EMAIL, ROLE_USER),
                ErrorCode.RECRUITMENT_COMPANY_ONLY);

        verify(recruitmentMapper, never()).findById(10);
    }

    @Test
    @DisplayName("CANCELLED 지원은 수락하거나 거절할 수 없다")
    void updateStatus_cancelled_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1))
                .willReturn(application(ApplicationStatus.CANCELLED, LocalDateTime.now(), null));

        assertStatusConflict("ACCEPTED");
    }

    @Test
    @DisplayName("이미 ACCEPTED 된 지원은 재변경할 수 없어 계약도 중복 생성되지 않는다")
    void updateStatus_accepted_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1))
                .willReturn(application(ApplicationStatus.ACCEPTED, null, null));

        assertStatusConflict("REJECTED");
        verify(contractMapper, never()).insert(any());
    }

    @Test
    @DisplayName("이미 REJECTED 된 지원은 재변경할 수 없다")
    void updateStatus_rejected_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1))
                .willReturn(application(ApplicationStatus.REJECTED, null, null));

        assertStatusConflict("ACCEPTED");
    }

    @Test
    @DisplayName("진행 중인 동일 계약이 있으면 지원 수락 계약을 중복 생성하지 않는다")
    void updateStatus_activeContractExists_fail() {
        given(recruitmentMapper.findById(10)).willReturn(openRecruitment());
        given(applicationMapper.findCompanyDetail(10, 1))
                .willReturn(application(ApplicationStatus.PENDING, null, null));
        given(contractMapper.existsActiveContract(10, USER_EMAIL)).willReturn(true);

        assertCustomException(
                () -> applicationService.updateStatus(
                        10, 1, statusRequest("ACCEPTED"), COMPANY_EMAIL, ROLE_COMPANY),
                ErrorCode.CONTRACT_ALREADY_EXISTS);

        verify(applicationMapper, never()).updateStatusIfPending(anyInt(), any());
        verify(contractMapper, never()).insert(any());
    }

    private void assertStatusConflict(String status) {
        assertCustomException(
                () -> applicationService.updateStatus(
                        10, 1, statusRequest(status), COMPANY_EMAIL, ROLE_COMPANY),
                ErrorCode.INVALID_APPLICATION_STATUS_CHANGE);
        verify(applicationMapper, never()).updateStatusIfPending(anyInt(), any());
    }

    private void stubDetail(ApplicationDTO application, List<Integer> portfolioIds) {
        given(applicationMapper.findByRecruitmentAndApplicant(10, USER_EMAIL)).willReturn(application);
        given(applicationPortfolioSnapshotMapper.findSummariesByApplicationId(application.getApplicationId()))
                .willReturn(portfolios(portfolioIds));
    }

    private void stubPortfolioSnapshotWrites(List<Integer> portfolioIds) {
        for (Integer portfolioId : portfolioIds) {
            given(applicationPortfolioSnapshotMapper.insertPortfolio(anyInt(), eq(portfolioId), anyInt()))
                    .willReturn(1);
        }
    }

    private void stubPortfolioProfile() {
        given(applicationProfileSnapshotMapper.findByApplicationId(1))
                .willReturn(ApplicationProfileSnapshotDTO.builder()
                .applicationId(1)
                .displayName("지원 홍길동")
                .jobCategory(JobCategory.IT)
                .profileFileId(11)
                .isPortfolioPublic(false)
                .intro("백엔드 개발자")
                .description("지원 당시 상세 소개")
                .build());
        given(applicationProfileSnapshotMapper.findTechStacksByApplicationId(1))
                .willReturn(List.of("Java", "Spring"));
    }

    private void stubCurrentPortfolioProfile() {
        given(portfolioService.getMyProfile(USER_EMAIL)).willReturn(PortfolioProfileDTO.builder()
                .freelancerEmail(USER_EMAIL)
                .displayName("지원 홍길동")
                .jobCategory(JobCategory.IT)
                .profileFileId(11)
                .isPortfolioPublic(false)
                .intro("백엔드 개발자")
                .description("지원 당시 상세 소개")
                .techStacks(List.of("Java", "Spring"))
                .updatedAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .build());
    }

    private ApplicationRequest request(String intro, List<Integer> portfolioIds) {
        ApplicationRequest request = new ApplicationRequest();
        request.setIntro(intro);
        request.setPortfolioIds(portfolioIds);
        return request;
    }

    private ApplicationRequest request(String intro,
                                       List<Integer> portfolioIds,
                                       ApplicationProfileSnapshotRequest profileRequest) {
        ApplicationRequest request = request(intro, portfolioIds);
        request.setPortfolioProfile(profileRequest);
        return request;
    }

    private ApplicationProfileSnapshotRequest profileRequest(String displayName,
                                                             Integer profileFileId,
                                                             List<String> techStacks) {
        ApplicationProfileSnapshotRequest request = new ApplicationProfileSnapshotRequest();
        request.setDisplayName(displayName);
        request.setJobCategory(JobCategory.IT);
        request.setProfileFileId(profileFileId);
        request.setIsPortfolioPublic(false);
        request.setIntro(" 백엔드 개발자 ");
        request.setDescription(" 지원 전용 상세 소개 ");
        request.setTechStacks(techStacks);
        return request;
    }

    private ApplicationStatusUpdateRequest statusRequest(String status) {
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus(status);
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
