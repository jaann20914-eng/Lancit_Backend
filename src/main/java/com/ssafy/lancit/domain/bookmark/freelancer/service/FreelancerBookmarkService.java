package com.ssafy.lancit.domain.bookmark.freelancer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.domain.bookmark.freelancer.dto.RecruitmentBookmarkResponse;
import com.ssafy.lancit.domain.bookmark.freelancer.mapper.FreelancerBookmarkMapper;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FreelancerBookmarkService {

    private static final String ROLE_USER = "USER";

    private final FreelancerBookmarkMapper freelancerBookmarkMapper;
    private final RecruitmentMapper recruitmentMapper;

    /**
     * 찜 토글 (있으면 취소, 없으면 추가)
     * return true = 찜 추가, false = 찜 취소
     */
    @Transactional
    public RecruitmentBookmarkResponse toggle(String freelancerEmail, String role, int recruitmentId) {
        requireUser(role);
        verifyRecruitmentExists(recruitmentId);

        boolean isBookmarked;
        if (freelancerBookmarkMapper.exists(freelancerEmail, recruitmentId)) {
            freelancerBookmarkMapper.delete(freelancerEmail, recruitmentId);
            isBookmarked = false;
        } else {
            freelancerBookmarkMapper.insert(freelancerEmail, recruitmentId);
            isBookmarked = true;
        }
        return RecruitmentBookmarkResponse.builder()
                .recruitmentId(recruitmentId)
                .isBookmarked(isBookmarked)
                .build();
    }

    /**
     * 특정 공고 찜 여부 확인
     */
    public boolean isBookmarked(String freelancerEmail, int recruitmentId) {
        return freelancerBookmarkMapper.exists(freelancerEmail, recruitmentId);
    }

    private void requireUser(String role) {
        if (!ROLE_USER.equals(role)) {
            throw new CustomException(ErrorCode.FREELANCER_ONLY);
        }
    }

    private void verifyRecruitmentExists(int recruitmentId) {
        RecruitmentDTO recruitment = recruitmentMapper.findById(recruitmentId);
        if (recruitment == null) {
            throw new CustomException(ErrorCode.RECRUITMENT_NOT_FOUND);
        }
    }
}
