package com.ssafy.lancit.domain.recruitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.bookmark.freelancer.mapper.FreelancerBookmarkMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.recruitment.post.dto.MyApplicationSummaryDTO;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentCreateRequest;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDetailResponse;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentListItemResponse;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentSearchCondition;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentStatusUpdateRequest;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentTechStackDTO;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentUpdateRequest;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.domain.recruitment.post.service.RecruitmentService;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.ApplicationStatus;
import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;
import com.ssafy.lancit.global.enums.RecruitmentSortType;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import com.ssafy.lancit.global.enums.RecruitmentViewStatus;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    private static final String COMPANY_EMAIL = "company@lancit.com";
    private static final String OTHER_COMPANY_EMAIL = "other@lancit.com";
    private static final String USER_EMAIL = "user@lancit.com";

    @InjectMocks
    private RecruitmentService recruitmentService;

    @Mock
    private RecruitmentMapper recruitmentMapper;

    @Mock
    private FreelancerBookmarkMapper freelancerBookmarkMapper;

    @Mock
    private FileService fileService;

    @Test
    @DisplayName("회사만 공고 등록 가능")
    void create_userRole_fail() {
        RecruitmentCreateRequest request = baseCreateRequest();

        assertCustomException(
                () -> recruitmentService.create(request, "user@lancit.com", "USER"),
                ErrorCode.RECRUITMENT_COMPANY_ONLY);

        verify(recruitmentMapper, never()).insertRecruitment(any(RecruitmentDTO.class));
    }

    @Test
    @DisplayName("공고 등록 성공 - 태그 중복 제거 및 이미지 연결")
    void create_success() {
        RecruitmentCreateRequest request = baseCreateRequest();
        request.setImageFileId(10);
        request.setTechStacks(List.of(" Java ", "Spring", "Java", " "));
        RecruitmentDTO saved = baseRecruitment(1);

        doAnswer(invocation -> {
            RecruitmentDTO dto = invocation.getArgument(0);
            dto.setRecruitmentId(1);
            return null;
        }).when(recruitmentMapper).insertRecruitment(any(RecruitmentDTO.class));
        given(recruitmentMapper.findById(1)).willReturn(saved);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of("Java", "Spring"));

        RecruitmentDetailResponse result = recruitmentService.create(request, COMPANY_EMAIL, "COMPANY");

        assertThat(result.getRecruitmentId()).isEqualTo(1);
        assertThat(result.getTechStacks()).containsExactly("Java", "Spring");
        verify(recruitmentMapper).insertRecruitment(any(RecruitmentDTO.class));
        verify(recruitmentMapper).insertTechStacks(1, List.of("Java", "Spring"));
        verify(fileService).attachToParent(10, FileParentType.RECRUITMENT_IMAGE, 1, COMPANY_EMAIL);
    }

    @Test
    @DisplayName("모집 종료일이 시작일보다 빠르면 등록 실패")
    void create_invalidRecruitmentPeriod_fail() {
        RecruitmentCreateRequest request = baseCreateRequest();
        request.setRecruitmentStartAt(LocalDateTime.of(2026, 6, 10, 0, 0));
        request.setRecruitmentEndAt(LocalDateTime.of(2026, 6, 9, 0, 0));

        assertCustomException(
                () -> recruitmentService.create(request, COMPANY_EMAIL, "COMPANY"),
                ErrorCode.INVALID_RECRUITMENT_PERIOD);

        verify(recruitmentMapper, never()).insertRecruitment(any(RecruitmentDTO.class));
    }

    @Test
    @DisplayName("활성 지원자가 있으면 공고 수정 불가")
    void update_activeApplications_fail() {
        RecruitmentUpdateRequest request = baseUpdateRequest();
        given(recruitmentMapper.findById(1)).willReturn(baseRecruitment(1));
        given(recruitmentMapper.countActiveApplications(1)).willReturn(1);

        assertCustomException(
                () -> recruitmentService.update(1, request, COMPANY_EMAIL, "COMPANY"),
                ErrorCode.RECRUITMENT_HAS_ACTIVE_APPLICATIONS);

        verify(recruitmentMapper, never()).updateRecruitment(eq(1), any(RecruitmentDTO.class));
    }

    @Test
    @DisplayName("공고 수정 성공 - 태그 전체 갱신 및 이미지 교체")
    void update_success() {
        RecruitmentUpdateRequest request = baseUpdateRequest();
        request.setImageFileId(20);
        request.setTechStacks(List.of("React", "Spring"));
        RecruitmentDTO existing = baseRecruitment(1);
        existing.setImageFileId(10);
        RecruitmentDTO updated = baseRecruitment(1);
        updated.setImageFileId(20);

        given(recruitmentMapper.findById(1)).willReturn(existing, updated);
        given(recruitmentMapper.countActiveApplications(1)).willReturn(0);
        given(recruitmentMapper.updateRecruitment(eq(1), any(RecruitmentDTO.class))).willReturn(1);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of("React", "Spring"));

        RecruitmentDetailResponse result = recruitmentService.update(1, request, COMPANY_EMAIL, "COMPANY");

        assertThat(result.getImageFileId()).isEqualTo(20);
        verify(recruitmentMapper).deleteTechStacks(1);
        verify(recruitmentMapper).insertTechStacks(1, List.of("React", "Spring"));
        verify(fileService).attachToParent(20, FileParentType.RECRUITMENT_IMAGE, 1, COMPANY_EMAIL);
        verify(fileService).deleteRecruitmentImageIfUnreferenced(10);
    }

    @Test
    @DisplayName("공고 이미지 제거 시 분리된 실제 파일을 정리한다")
    void update_removeImage_success() {
        RecruitmentUpdateRequest request = baseUpdateRequest();
        request.setImageFileId(null);
        RecruitmentDTO existing = baseRecruitment(1);
        existing.setImageFileId(10);
        RecruitmentDTO updated = baseRecruitment(1);

        given(recruitmentMapper.findById(1)).willReturn(existing, updated);
        given(recruitmentMapper.countActiveApplications(1)).willReturn(0);
        given(recruitmentMapper.updateRecruitment(eq(1), any(RecruitmentDTO.class))).willReturn(1);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of());

        recruitmentService.update(1, request, COMPANY_EMAIL, "COMPANY");

        verify(fileService).deleteRecruitmentImageIfUnreferenced(10);
        verify(fileService, never()).attachToParent(any(), eq(FileParentType.RECRUITMENT_IMAGE), eq(1), eq(COMPANY_EMAIL));
    }

    @Test
    @DisplayName("공고 삭제 시 연결되던 이미지를 실제 파일 정리 대상으로 넘긴다")
    void delete_withImage_success() {
        RecruitmentDTO existing = baseRecruitment(1);
        existing.setImageFileId(10);
        given(recruitmentMapper.findById(1)).willReturn(existing);
        given(recruitmentMapper.countActiveApplications(1)).willReturn(0);
        given(recruitmentMapper.softDeleteRecruitment(1)).willReturn(1);

        recruitmentService.delete(1, COMPANY_EMAIL, "COMPANY");

        verify(fileService).deleteRecruitmentImageIfUnreferenced(10);
    }

    @Test
    @DisplayName("취소 상태의 공고는 활성 지원자가 있어도 삭제 가능")
    void delete_cancelledRecruitmentWithActiveApplications_success() {
        RecruitmentDTO existing = baseRecruitment(1);
        existing.setStatus(RecruitmentStatus.CANCELLED);
        existing.setApplicantCount(3);
        given(recruitmentMapper.findById(1)).willReturn(existing);
        given(recruitmentMapper.softDeleteRecruitment(1)).willReturn(1);

        recruitmentService.delete(1, COMPANY_EMAIL, "COMPANY");

        verify(recruitmentMapper, never()).countActiveApplications(1);
        verify(recruitmentMapper).softDeleteRecruitment(1);
    }

    @Test
    @DisplayName("취소 상태의 내 공고는 활성 지원자가 있어도 삭제 권한이 true")
    void getOne_cancelledRecruitmentWithActiveApplications_canDelete() {
        RecruitmentDTO dto = baseRecruitment(1);
        dto.setStatus(RecruitmentStatus.CANCELLED);
        dto.setApplicantCount(3);
        given(recruitmentMapper.findById(1)).willReturn(dto);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of());

        RecruitmentDetailResponse result = recruitmentService.getOne(1, COMPANY_EMAIL, "COMPANY");

        assertThat(result.getCanDelete()).isTrue();
        assertThat(result.getPermission().getCanDelete()).isTrue();
    }

    @Test
    @DisplayName("공고 복사 원본에는 기존 이미지 ID를 포함하지 않는다")
    void getCopySource_excludesImage_success() {
        RecruitmentDTO existing = baseRecruitment(1);
        existing.setImageFileId(10);
        given(recruitmentMapper.findById(1)).willReturn(existing);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of("Java"));

        RecruitmentCreateRequest result = recruitmentService.getCopySource(1, COMPANY_EMAIL, "COMPANY");

        assertThat(result.getImageFileId()).isNull();
        assertThat(result.getTechStacks()).containsExactly("Java");
    }

    @Test
    @DisplayName("내 공고가 아니면 상태 변경 불가")
    void updateStatus_otherOwner_fail() {
        RecruitmentStatusUpdateRequest request = new RecruitmentStatusUpdateRequest();
        request.setStatus("CLOSED");
        RecruitmentDTO existing = baseRecruitment(1);
        existing.setCompanyEmail(OTHER_COMPANY_EMAIL);
        given(recruitmentMapper.findById(1)).willReturn(existing);

        assertCustomException(
                () -> recruitmentService.updateStatus(1, request, COMPANY_EMAIL, "COMPANY"),
                ErrorCode.RECRUITMENT_FORBIDDEN);

        verify(recruitmentMapper, never()).updateStatus(1, RecruitmentStatus.CLOSED);
    }

    @Test
    @DisplayName("EXPIRED는 저장 상태로 직접 변경할 수 없음")
    void updateStatus_expired_fail() {
        RecruitmentStatusUpdateRequest request = new RecruitmentStatusUpdateRequest();
        request.setStatus("EXPIRED");

        assertCustomException(
                () -> recruitmentService.updateStatus(1, request, COMPANY_EMAIL, "COMPANY"),
                ErrorCode.INVALID_RECRUITMENT_STATUS);

        verify(recruitmentMapper, never()).findById(1);
        verify(recruitmentMapper, never()).updateStatus(eq(1), any(RecruitmentStatus.class));
    }

    @Test
    @DisplayName("잘못된 문자열은 공고 상태 변경 실패")
    void updateStatus_invalidString_fail() {
        RecruitmentStatusUpdateRequest request = new RecruitmentStatusUpdateRequest();
        request.setStatus("DONE");

        assertCustomException(
                () -> recruitmentService.updateStatus(1, request, COMPANY_EMAIL, "COMPANY"),
                ErrorCode.INVALID_RECRUITMENT_STATUS);

        verify(recruitmentMapper, never()).findById(1);
        verify(recruitmentMapper, never()).updateStatus(eq(1), any(RecruitmentStatus.class));
    }

    @Test
    @DisplayName("OPEN, CLOSED, CANCELLED는 상태 변경 가능")
    void updateStatus_allowedStatuses_success() {
        for (RecruitmentStatus status : List.of(RecruitmentStatus.OPEN, RecruitmentStatus.CLOSED, RecruitmentStatus.CANCELLED)) {
            RecruitmentStatusUpdateRequest request = new RecruitmentStatusUpdateRequest();
            request.setStatus(status.name().toLowerCase());
            RecruitmentDTO existing = baseRecruitment(1);
            RecruitmentDTO updated = baseRecruitment(1);
            updated.setStatus(status);

            given(recruitmentMapper.findById(1)).willReturn(existing, updated);
            given(recruitmentMapper.updateStatus(1, status)).willReturn(1);
            given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of());

            RecruitmentDetailResponse result = recruitmentService.updateStatus(1, request, COMPANY_EMAIL, "COMPANY");

            assertThat(result.getStatus()).isEqualTo(status);
            verify(recruitmentMapper).updateStatus(1, status);
        }
    }

    @Test
    @DisplayName("공고 목록 조회 페이징 및 기술 태그 매핑")
    void getList_success() {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(2);
        pageRequest.setSize(1);
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setKeyword(" 공고 ");
        condition.setSort(RecruitmentSortType.BUDGET);
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(3L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1)))
                .willReturn(List.of(new RecruitmentTechStackDTO(1, "Spring")));

        PageResponse<RecruitmentListItemResponse> result =
                recruitmentService.getList(condition, pageRequest, COMPANY_EMAIL, "COMPANY");

        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(result.getContent().get(0).getTechStacks()).containsExactly("Spring");
        assertThat(result.getContent().get(0).getIsMine()).isTrue();
        assertThat(result.getContent().get(0).getCanApply()).isFalse();
        assertThat(result.getContent().get(0).getIsApplied()).isFalse();
        assertThat(result.getContent().get(0).getIsBookmarked()).isFalse();
        assertThat(condition.getKeyword()).isEqualTo("공고");
        assertThat(condition.getTab()).isEqualTo("ALL");
    }

    @Test
    @DisplayName("공고 목록 조회 - 프리랜서 기준 찜 여부 계산")
    void getList_userBookmarkedFlag_success() {
        PageRequest pageRequest = new PageRequest();
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(1L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1))).willReturn(List.of());
        given(freelancerBookmarkMapper.findBookmarkedRecruitmentIds(USER_EMAIL, List.of(1)))
                .willReturn(List.of(1));

        PageResponse<RecruitmentListItemResponse> result =
                recruitmentService.getList(condition, pageRequest, USER_EMAIL, "USER");

        assertThat(result.getContent().get(0).getIsBookmarked()).isTrue();
    }

    @Test
    @DisplayName("공고 목록 조회 - 프리랜서 기준 지원 여부 계산")
    void getList_userAppliedFlag_success() {
        PageRequest pageRequest = new PageRequest();
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(1L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1))).willReturn(List.of());
        given(recruitmentMapper.findMyApplicationSummaries(USER_EMAIL, List.of(1)))
                .willReturn(List.of(myApplication(1, ApplicationStatus.PENDING)));

        PageResponse<RecruitmentListItemResponse> result =
                recruitmentService.getList(condition, pageRequest, USER_EMAIL, "USER");

        assertThat(result.getContent().get(0).getIsApplied()).isTrue();
        assertThat(result.getContent().get(0).getCanApply()).isFalse();
        assertThat(result.getContent().get(0).getMyApplicationStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(result.getContent().get(0).getMyApplicationId()).isEqualTo(101);
    }

    @Test
    @DisplayName("공고 목록 조회 - 취소 지원은 상태와 ID를 반환하고 열린 공고에 재지원 가능")
    void getList_cancelledApplicationCanReapply_success() {
        PageRequest pageRequest = new PageRequest();
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(1L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1))).willReturn(List.of());
        given(recruitmentMapper.findMyApplicationSummaries(USER_EMAIL, List.of(1)))
                .willReturn(List.of(myApplication(1, ApplicationStatus.CANCELLED)));

        PageResponse<RecruitmentListItemResponse> result =
                recruitmentService.getList(condition, pageRequest, USER_EMAIL, "USER");

        RecruitmentListItemResponse item = result.getContent().get(0);
        assertThat(item.getIsApplied()).isFalse();
        assertThat(item.getCanApply()).isTrue();
        assertThat(item.getMyApplicationStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(item.getMyApplicationId()).isEqualTo(101);
    }

    @Test
    @DisplayName("공고 목록 조회 - 프리랜서가 미지원 OPEN 공고는 지원 가능")
    void getList_userCanApplyWhenOpenAndNotApplied_success() {
        PageRequest pageRequest = new PageRequest();
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(1L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1))).willReturn(List.of());

        PageResponse<RecruitmentListItemResponse> result =
                recruitmentService.getList(condition, pageRequest, USER_EMAIL, "USER");

        RecruitmentListItemResponse item = result.getContent().get(0);
        assertThat(item.getIsApplied()).isFalse();
        assertThat(item.getCanApply()).isTrue();
    }

    @Test
    @DisplayName("공고 목록 조회 - 익명 사용자는 지원/찜/지원가능 여부가 false")
    void getList_anonymousFlags_success() {
        PageRequest pageRequest = new PageRequest();
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(1L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1))).willReturn(List.of());

        PageResponse<RecruitmentListItemResponse> result =
                recruitmentService.getList(condition, pageRequest, null, null);

        RecruitmentListItemResponse item = result.getContent().get(0);
        assertThat(item.getIsApplied()).isFalse();
        assertThat(item.getIsBookmarked()).isFalse();
        assertThat(item.getCanApply()).isFalse();
    }

    @Test
    @DisplayName("지원 탭은 프리랜서가 지원한 공고만 조회하고 isApplied=true")
    void getList_appliedTab_success() {
        PageRequest pageRequest = new PageRequest();
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setTab("applied");
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(1L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1))).willReturn(List.of());
        given(recruitmentMapper.findMyApplicationSummaries(USER_EMAIL, List.of(1)))
                .willReturn(List.of(myApplication(1, ApplicationStatus.PENDING)));

        PageResponse<RecruitmentListItemResponse> result =
                recruitmentService.getList(condition, pageRequest, USER_EMAIL, "USER");

        assertThat(condition.getTab()).isEqualTo("APPLIED");
        assertThat(condition.getCurrentEmail()).isEqualTo(USER_EMAIL);
        assertThat(result.getContent().get(0).getIsApplied()).isTrue();
        assertThat(result.getContent().get(0).getCanApply()).isFalse();
    }

    @Test
    @DisplayName("지원 탭은 인증된 프리랜서만 조회 가능")
    void getList_appliedTabAnonymous_fail() {
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setTab("APPLIED");

        assertCustomException(
                () -> recruitmentService.getList(condition, new PageRequest(), null, null),
                ErrorCode.UNAUTHORIZED);

        verify(recruitmentMapper, never()).findList(any(), any());
    }

    @Test
    @DisplayName("지원 탭은 회사 계정으로 조회 불가")
    void getList_appliedTabCompany_fail() {
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setTab("APPLIED");

        assertCustomException(
                () -> recruitmentService.getList(condition, new PageRequest(), COMPANY_EMAIL, "COMPANY"),
                ErrorCode.FREELANCER_ONLY);

        verify(recruitmentMapper, never()).findList(any(), any());
    }

    @Test
    @DisplayName("찜 탭은 익명 사용자로 조회 불가")
    void getList_bookmarkedTabAnonymous_fail() {
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setTab("BOOKMARKED");

        assertCustomException(
                () -> recruitmentService.getList(condition, new PageRequest(), null, null),
                ErrorCode.UNAUTHORIZED);

        verify(recruitmentMapper, never()).findList(any(), any());
    }

    @Test
    @DisplayName("찜 탭은 프리랜서만 조회 가능")
    void getList_bookmarkedTabCompany_fail() {
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setTab("BOOKMARKED");

        assertCustomException(
                () -> recruitmentService.getList(condition, new PageRequest(), COMPANY_EMAIL, "COMPANY"),
                ErrorCode.FREELANCER_ONLY);

        verify(recruitmentMapper, never()).findList(any(), any());
    }

    @Test
    @DisplayName("찜 탭은 bookmark 기준 목록 조건으로 조회")
    void getList_bookmarkedTab_success() {
        PageRequest pageRequest = new PageRequest();
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setTab("bookmarked");
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(1L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1))).willReturn(List.of());
        given(freelancerBookmarkMapper.findBookmarkedRecruitmentIds(USER_EMAIL, List.of(1)))
                .willReturn(List.of(1));

        PageResponse<RecruitmentListItemResponse> result =
                recruitmentService.getList(condition, pageRequest, USER_EMAIL, "USER");

        assertThat(condition.getTab()).isEqualTo("BOOKMARKED");
        assertThat(condition.getCurrentEmail()).isEqualTo(USER_EMAIL);
        assertThat(result.getContent().get(0).getIsBookmarked()).isTrue();
        verify(freelancerBookmarkMapper, never())
                .findBookmarkedRecruitments(any(), any(), any());
    }

    @Test
    @DisplayName("직종 필터와 탭 조건은 함께 전달")
    void getList_tabWithJobCategory_success() {
        PageRequest pageRequest = new PageRequest();
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setTab("APPLIED");
        condition.setJobCategory(JobCategory.IT);
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(1L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1))).willReturn(List.of());
        given(recruitmentMapper.findMyApplicationSummaries(USER_EMAIL, List.of(1)))
                .willReturn(List.of(myApplication(1, ApplicationStatus.PENDING)));

        recruitmentService.getList(condition, pageRequest, USER_EMAIL, "USER");

        assertThat(condition.getTab()).isEqualTo("APPLIED");
        assertThat(condition.getJobCategory()).isEqualTo(JobCategory.IT);
        verify(recruitmentMapper).findList(condition, pageRequest);
        verify(recruitmentMapper).countList(condition);
    }

    @Test
    @DisplayName("정렬 조건은 LATEST, DEADLINE, BUDGET 값을 유지")
    void getList_sortValues_success() {
        for (RecruitmentSortType sort : List.of(
                RecruitmentSortType.LATEST,
                RecruitmentSortType.DEADLINE,
                RecruitmentSortType.BUDGET)) {
            PageRequest pageRequest = new PageRequest();
            RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
            condition.setSort(sort);

            given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of());
            given(recruitmentMapper.countList(condition)).willReturn(0L);

            recruitmentService.getList(condition, pageRequest, null, null);

            assertThat(condition.getSort()).isEqualTo(sort);
            assertThat(condition.getSafeSort()).isEqualTo(sort.name());
        }
    }

    @Test
    @DisplayName("찜한 공고 목록 조회 - 응답 구조와 찜 여부")
    void getBookmarkedList_success() {
        PageRequest pageRequest = new PageRequest();
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        RecruitmentDTO dto = baseRecruitment(1);

        given(recruitmentMapper.findList(condition, pageRequest)).willReturn(List.of(dto));
        given(recruitmentMapper.countList(condition)).willReturn(1L);
        given(recruitmentMapper.findTechStacksByRecruitmentIds(List.of(1)))
                .willReturn(List.of(new RecruitmentTechStackDTO(1, "Spring")));
        given(freelancerBookmarkMapper.findBookmarkedRecruitmentIds(USER_EMAIL, List.of(1)))
                .willReturn(List.of(1));

        PageResponse<RecruitmentListItemResponse> result =
                recruitmentService.getBookmarkedList(USER_EMAIL, "USER", condition, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getRecruitmentId()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTechStacks()).containsExactly("Spring");
        assertThat(result.getContent().get(0).getIsBookmarked()).isTrue();
        assertThat(condition.getTab()).isEqualTo("BOOKMARKED");
    }

    @Test
    @DisplayName("찜한 공고 목록은 프리랜서만 조회 가능")
    void getBookmarkedList_companyRole_fail() {
        assertCustomException(
                () -> recruitmentService.getBookmarkedList(COMPANY_EMAIL, "COMPANY",
                        new RecruitmentSearchCondition(), new PageRequest()),
                ErrorCode.FREELANCER_ONLY);
    }

    @Test
    @DisplayName("잘못된 상태 필터면 목록 조회 실패")
    void getList_invalidStatus_fail() {
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setStatus("DONE");

        assertCustomException(
                () -> recruitmentService.getList(condition, new PageRequest(), null, null),
                ErrorCode.INVALID_RECRUITMENT_STATUS);

        verify(recruitmentMapper, never()).findList(any(), any());
    }

    @Test
    @DisplayName("잘못된 탭이면 목록 조회 실패")
    void getList_invalidTab_fail() {
        RecruitmentSearchCondition condition = new RecruitmentSearchCondition();
        condition.setTab("LIKED");

        assertCustomException(
                () -> recruitmentService.getList(condition, new PageRequest(), USER_EMAIL, "USER"),
                ErrorCode.INVALID_RECRUITMENT_TAB);

        verify(recruitmentMapper, never()).findList(any(), any());
    }

    @Test
    @DisplayName("만료된 모집중 공고는 viewStatus EXPIRED")
    void getOne_expiredViewStatus_success() {
        RecruitmentDTO dto = baseRecruitment(1);
        dto.setStatus(RecruitmentStatus.OPEN);
        dto.setRecruitmentEndAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        given(recruitmentMapper.findById(1)).willReturn(dto);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of());

        RecruitmentDetailResponse result = recruitmentService.getOne(1, USER_EMAIL, "USER");

        assertThat(result.getViewStatus()).isEqualTo(RecruitmentViewStatus.EXPIRED);
        assertThat(result.getCanApply()).isFalse();
        assertThat(result.getIsBookmarked()).isFalse();
    }

    @Test
    @DisplayName("공고 상세 조회 - 프리랜서 기준 찜 여부 계산")
    void getOne_userBookmarkedFlag_success() {
        RecruitmentDTO dto = baseRecruitment(1);
        given(recruitmentMapper.findById(1)).willReturn(dto);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of());
        given(freelancerBookmarkMapper.exists(USER_EMAIL, 1)).willReturn(true);

        RecruitmentDetailResponse result = recruitmentService.getOne(1, USER_EMAIL, "USER");

        assertThat(result.getIsBookmarked()).isTrue();
    }

    @Test
    @DisplayName("공고 상세 조회 - 진행 중인 내 지원 상태와 ID 반환")
    void getOne_pendingApplicationStatus_success() {
        RecruitmentDTO dto = baseRecruitment(1);
        given(recruitmentMapper.findById(1)).willReturn(dto);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of());
        given(recruitmentMapper.findMyApplicationSummaries(USER_EMAIL, List.of(1)))
                .willReturn(List.of(myApplication(1, ApplicationStatus.PENDING)));

        RecruitmentDetailResponse result = recruitmentService.getOne(1, USER_EMAIL, "USER");

        assertThat(result.getIsApplied()).isTrue();
        assertThat(result.getCanApply()).isFalse();
        assertThat(result.getMyApplicationStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(result.getMyApplicationId()).isEqualTo(101);
        assertThat(result.getPermission().getMyApplicationStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(result.getPermission().getMyApplicationId()).isEqualTo(101);
    }

    @Test
    @DisplayName("공고 상세 조회 - 취소한 내 지원 상태와 ID를 유지하고 재지원 허용")
    void getOne_cancelledApplicationStatus_success() {
        RecruitmentDTO dto = baseRecruitment(1);
        given(recruitmentMapper.findById(1)).willReturn(dto);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of());
        given(recruitmentMapper.findMyApplicationSummaries(USER_EMAIL, List.of(1)))
                .willReturn(List.of(myApplication(1, ApplicationStatus.CANCELLED)));

        RecruitmentDetailResponse result = recruitmentService.getOne(1, USER_EMAIL, "USER");

        assertThat(result.getIsApplied()).isFalse();
        assertThat(result.getCanApply()).isTrue();
        assertThat(result.getMyApplicationStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        assertThat(result.getMyApplicationId()).isEqualTo(101);
        assertThat(result.getPermission().getIsApplied()).isFalse();
        assertThat(result.getPermission().getCanApply()).isTrue();
    }

    private RecruitmentCreateRequest baseCreateRequest() {
        RecruitmentCreateRequest request = new RecruitmentCreateRequest();
        request.setTitle("공고 제목");
        request.setSummary("한줄 소개");
        request.setContent("공고 내용");
        request.setRequirements("요구사항");
        request.setJobCategory(JobCategory.IT);
        request.setRecruitmentCategory(RecruitmentCategory.WEB_APP);
        request.setBudget(1_000_000);
        request.setWorkLocation("서울");
        request.setContractStartAt(LocalDateTime.of(2026, 7, 1, 0, 0));
        request.setContractEndAt(LocalDateTime.of(2026, 8, 1, 0, 0));
        request.setRecruitmentStartAt(LocalDateTime.of(2026, 6, 1, 0, 0));
        request.setRecruitmentEndAt(LocalDateTime.of(2026, 6, 30, 0, 0));
        return request;
    }

    private RecruitmentUpdateRequest baseUpdateRequest() {
        RecruitmentUpdateRequest request = new RecruitmentUpdateRequest();
        request.setTitle("수정 공고");
        request.setSummary("수정 소개");
        request.setContent("수정 내용");
        request.setRequirements("수정 요구사항");
        request.setJobCategory(JobCategory.IT);
        request.setRecruitmentCategory(RecruitmentCategory.WEB_APP);
        request.setBudget(2_000_000);
        request.setWorkLocation("원격");
        request.setContractStartAt(LocalDateTime.of(2026, 7, 1, 0, 0));
        request.setContractEndAt(LocalDateTime.of(2026, 8, 1, 0, 0));
        request.setRecruitmentStartAt(LocalDateTime.of(2026, 6, 1, 0, 0));
        request.setRecruitmentEndAt(LocalDateTime.of(2026, 6, 30, 0, 0));
        return request;
    }

    private RecruitmentDTO baseRecruitment(int recruitmentId) {
        return RecruitmentDTO.builder()
                .recruitmentId(recruitmentId)
                .companyEmail(COMPANY_EMAIL)
                .companyName("테스트회사")
                .title("공고 제목")
                .summary("한줄 소개")
                .content("공고 내용")
                .requirements("요구사항")
                .jobCategory(JobCategory.IT)
                .recruitmentCategory(RecruitmentCategory.WEB_APP)
                .status(RecruitmentStatus.OPEN)
                .workLocation("서울")
                .budget(1_000_000)
                .recruitmentStartAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .recruitmentEndAt(LocalDateTime.of(2026, 6, 30, 0, 0))
                .contractStartAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .contractEndAt(LocalDateTime.of(2026, 8, 1, 0, 0))
                .createdAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .applicantCount(0)
                .build();
    }

    private MyApplicationSummaryDTO myApplication(int recruitmentId, ApplicationStatus status) {
        return new MyApplicationSummaryDTO(recruitmentId, recruitmentId + 100, status);
    }

    private void assertCustomException(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }
}
