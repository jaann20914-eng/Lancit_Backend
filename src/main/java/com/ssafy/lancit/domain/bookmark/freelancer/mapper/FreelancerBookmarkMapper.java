package com.ssafy.lancit.domain.bookmark.freelancer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentSearchCondition;

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
    List<RecruitmentDTO> findBookmarkedRecruitments(
            @Param("freelancerEmail") String freelancerEmail,
            @Param("condition") RecruitmentSearchCondition condition,
            @Param("pageRequest") PageRequest pageRequest);

    // 전체 카운트
    long countBookmarkedRecruitments(@Param("freelancerEmail") String freelancerEmail,
                                     @Param("condition") RecruitmentSearchCondition condition);

    // 공고 목록의 현재 사용자 찜 상태 batch 조회
    List<Integer> findBookmarkedRecruitmentIds(
            @Param("freelancerEmail") String freelancerEmail,
            @Param("recruitmentIds") List<Integer> recruitmentIds);
}
