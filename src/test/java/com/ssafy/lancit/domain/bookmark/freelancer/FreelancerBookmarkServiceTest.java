package com.ssafy.lancit.domain.bookmark.freelancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.bookmark.freelancer.dto.RecruitmentBookmarkResponse;
import com.ssafy.lancit.domain.bookmark.freelancer.mapper.FreelancerBookmarkMapper;
import com.ssafy.lancit.domain.bookmark.freelancer.service.FreelancerBookmarkService;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.JobCategory;
import com.ssafy.lancit.global.enums.RecruitmentCategory;
import com.ssafy.lancit.global.enums.RecruitmentStatus;

@ExtendWith(MockitoExtension.class)
class FreelancerBookmarkServiceTest {

    private static final String USER_EMAIL = "user@lancit.com";
    private static final String ROLE_USER = "USER";
    private static final String ROLE_COMPANY = "COMPANY";

    @InjectMocks
    private FreelancerBookmarkService freelancerBookmarkService;

    @Mock
    private FreelancerBookmarkMapper freelancerBookmarkMapper;

    @Mock
    private RecruitmentMapper recruitmentMapper;

    @Test
    @DisplayName("프리랜서가 찜하지 않은 공고를 토글하면 등록")
    void toggle_notBookmarked_insert() {
        given(recruitmentMapper.findById(1)).willReturn(baseRecruitment(1));
        given(freelancerBookmarkMapper.exists(USER_EMAIL, 1)).willReturn(false);

        RecruitmentBookmarkResponse result = freelancerBookmarkService.toggle(USER_EMAIL, ROLE_USER, 1);

        assertThat(result.getRecruitmentId()).isEqualTo(1);
        assertThat(result.getIsBookmarked()).isTrue();
        verify(freelancerBookmarkMapper).insert(USER_EMAIL, 1);
        verify(freelancerBookmarkMapper, never()).delete(USER_EMAIL, 1);
    }

    @Test
    @DisplayName("프리랜서가 이미 찜한 공고를 토글하면 해제")
    void toggle_alreadyBookmarked_delete() {
        given(recruitmentMapper.findById(1)).willReturn(baseRecruitment(1));
        given(freelancerBookmarkMapper.exists(USER_EMAIL, 1)).willReturn(true);

        RecruitmentBookmarkResponse result = freelancerBookmarkService.toggle(USER_EMAIL, ROLE_USER, 1);

        assertThat(result.getRecruitmentId()).isEqualTo(1);
        assertThat(result.getIsBookmarked()).isFalse();
        verify(freelancerBookmarkMapper).delete(USER_EMAIL, 1);
        verify(freelancerBookmarkMapper, never()).insert(USER_EMAIL, 1);
    }

    @Test
    @DisplayName("회사 계정은 공고 찜 불가")
    void toggle_companyRole_fail() {
        assertCustomException(
                () -> freelancerBookmarkService.toggle("company@lancit.com", ROLE_COMPANY, 1),
                ErrorCode.FREELANCER_ONLY);

        verify(recruitmentMapper, never()).findById(1);
        verify(freelancerBookmarkMapper, never()).insert("company@lancit.com", 1);
    }

    @Test
    @DisplayName("존재하지 않는 공고는 찜 불가")
    void toggle_recruitmentNotFound_fail() {
        given(recruitmentMapper.findById(999)).willReturn(null);

        assertCustomException(
                () -> freelancerBookmarkService.toggle(USER_EMAIL, ROLE_USER, 999),
                ErrorCode.RECRUITMENT_NOT_FOUND);

        verify(freelancerBookmarkMapper, never()).insert(USER_EMAIL, 999);
    }

    @Test
    @DisplayName("삭제된 공고는 찜 불가")
    void toggle_deletedRecruitment_fail() {
        given(recruitmentMapper.findById(1)).willReturn(null);

        assertCustomException(
                () -> freelancerBookmarkService.toggle(USER_EMAIL, ROLE_USER, 1),
                ErrorCode.RECRUITMENT_NOT_FOUND);

        verify(freelancerBookmarkMapper, never()).insert(USER_EMAIL, 1);
    }

    private RecruitmentDTO baseRecruitment(int recruitmentId) {
        return RecruitmentDTO.builder()
                .recruitmentId(recruitmentId)
                .companyEmail("company@lancit.com")
                .title("공고 제목")
                .summary("한줄 소개")
                .content("공고 내용")
                .jobCategory(JobCategory.IT)
                .recruitmentCategory(RecruitmentCategory.WEB_APP)
                .status(RecruitmentStatus.OPEN)
                .build();
    }

    private void assertCustomException(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }
}
