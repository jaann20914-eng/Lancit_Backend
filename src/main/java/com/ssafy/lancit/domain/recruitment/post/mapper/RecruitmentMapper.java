package com.ssafy.lancit.domain.recruitment.post.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentSearchCondition;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentTechStackDTO;
import com.ssafy.lancit.global.enums.RecruitmentStatus;

@Mapper
public interface RecruitmentMapper {
    void insertRecruitment(RecruitmentDTO dto);

    int updateRecruitment(@Param("recruitmentId") int recruitmentId,
                          @Param("dto") RecruitmentDTO dto);

    int softDeleteRecruitment(@Param("recruitmentId") int recruitmentId);

    int updateStatus(@Param("recruitmentId") int recruitmentId,
                     @Param("status") RecruitmentStatus status);

    RecruitmentDTO findById(@Param("recruitmentId") int recruitmentId);

    List<RecruitmentDTO> findList(@Param("condition") RecruitmentSearchCondition condition,
                                  @Param("pageRequest") PageRequest pageRequest);

    long countList(@Param("condition") RecruitmentSearchCondition condition);

    List<RecruitmentDTO> findMyList(@Param("companyEmail") String companyEmail,
                                    @Param("condition") RecruitmentSearchCondition condition,
                                    @Param("pageRequest") PageRequest pageRequest);

    long countMyList(@Param("companyEmail") String companyEmail,
                     @Param("condition") RecruitmentSearchCondition condition);

    void insertTechStacks(@Param("recruitmentId") int recruitmentId,
                          @Param("techStacks") List<String> techStacks);

    void deleteTechStacks(@Param("recruitmentId") int recruitmentId);

    List<String> findTechStacksByRecruitmentId(@Param("recruitmentId") int recruitmentId);

    List<RecruitmentTechStackDTO> findTechStacksByRecruitmentIds(
            @Param("recruitmentIds") List<Integer> recruitmentIds);

    int countActiveApplications(@Param("recruitmentId") int recruitmentId);

    String findOwnerEmailById(@Param("recruitmentId") int recruitmentId);
}
