package com.ssafy.lancit.domain.bookmark.freelancer.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.bookmark.freelancer.mapper.FreelancerBookmarkMapper;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FreelancerBookmarkService {

    private final FreelancerBookmarkMapper freelancerBookmarkMapper;

    /**
     * 찜 토글 (있으면 취소, 없으면 추가)
     * return true = 찜 추가, false = 찜 취소
     */
    @Transactional
    public boolean toggle(String freelancerEmail, int recruitmentId) {
        if (freelancerBookmarkMapper.exists(freelancerEmail, recruitmentId)) {
            freelancerBookmarkMapper.delete(freelancerEmail, recruitmentId);
            return false;
        } else {
            freelancerBookmarkMapper.insert(freelancerEmail, recruitmentId);
            return true;
        }
    }

    /**
     * 찜한 공고 목록 조회 (페이지네이션)
     */
    public PageResponse<RecruitmentDTO> getBookmarkedList(String freelancerEmail,
                                                           PageRequest pageRequest) {
        List<RecruitmentDTO> list = freelancerBookmarkMapper
                .findBookmarkedList(freelancerEmail, pageRequest);
        long total = freelancerBookmarkMapper.countBookmarkedList(freelancerEmail);
        return PageResponse.of(list, total, pageRequest);
    }

    /**
     * 특정 공고 찜 여부 확인
     */
    public boolean isBookmarked(String freelancerEmail, int recruitmentId) {
        return freelancerBookmarkMapper.exists(freelancerEmail, recruitmentId);
    }
}