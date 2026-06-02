package com.ssafy.lancit.domain.bookmark.freelancer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;

@Mapper
public interface FreelancerBookmarkMapper {

    // 찜 추가
    void insert(@Param("freelancerEmail") String freelancerEmail,
                @Param("recruitmentId") int recruitmentId);

    // 찜 취소
    void delete(@Param("freelancerEmail") String freelancerEmail,
                @Param("recruitmentId") int recruitmentId);

    // 찜 여부 확인
    boolean exists(@Param("freelancerEmail") String freelancerEmail,
                   @Param("recruitmentId") int recruitmentId);

    // 찜한 공고 목록 (RecruitmentDTO 로 조인해서 반환)
    List<RecruitmentDTO> findBookmarkedList(
            @Param("freelancerEmail") String freelancerEmail,
            @Param("pageRequest") PageRequest pageRequest);

    // 전체 카운트
    long countBookmarkedList(@Param("freelancerEmail") String freelancerEmail);
}