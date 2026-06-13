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
import com.ssafy.lancit.domain.file.service.FileService;
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
import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;
import com.ssafy.lancit.global.enums.RecruitmentSortType;
import com.ssafy.lancit.global.enums.RecruitmentStatus;
import com.ssafy.lancit.global.enums.RecruitmentViewStatus;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    private static final String COMPANY_EMAIL = "company@lancit.com";
    private static final String OTHER_COMPANY_EMAIL = "other@lancit.com";

    @InjectMocks
    private RecruitmentService recruitmentService;

    @Mock
    private RecruitmentMapper recruitmentMapper;

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
        verify(fileService).detach(10);
        verify(fileService).attachToParent(20, FileParentType.RECRUITMENT_IMAGE, 1, COMPANY_EMAIL);
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
        assertThat(condition.getKeyword()).isEqualTo("공고");
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
    @DisplayName("만료된 모집중 공고는 viewStatus EXPIRED")
    void getOne_expiredViewStatus_success() {
        RecruitmentDTO dto = baseRecruitment(1);
        dto.setStatus(RecruitmentStatus.OPEN);
        dto.setRecruitmentEndAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        given(recruitmentMapper.findById(1)).willReturn(dto);
        given(recruitmentMapper.findTechStacksByRecruitmentId(1)).willReturn(List.of());

        RecruitmentDetailResponse result = recruitmentService.getOne(1, "user@lancit.com", "USER");

        assertThat(result.getViewStatus()).isEqualTo(RecruitmentViewStatus.EXPIRED);
        assertThat(result.getCanApply()).isFalse();
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

    private void assertCustomException(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }
}
