package com.ssafy.lancit.domain.recruitment.application.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDTO;

@Mapper
public interface ApplicationMapper {
    void insert(ApplicationDTO dto);

    boolean existsByRecruitmentAndApplicant(@Param("recruitmentId") int recruitmentId,
                                            @Param("applicantEmail") String applicantEmail);

    ApplicationDTO findByRecruitmentAndApplicant(@Param("recruitmentId") int recruitmentId,
                                                 @Param("applicantEmail") String applicantEmail);

    ApplicationDTO findById(@Param("applicationId") int applicationId);

    int updateIntro(@Param("applicationId") int applicationId,
                    @Param("intro") String intro);

    int cancel(@Param("applicationId") int applicationId);
}
