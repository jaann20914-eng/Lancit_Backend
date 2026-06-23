package com.ssafy.lancit.domain.recruitment.application.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDTO;
import com.ssafy.lancit.global.enums.ApplicationStatus;

@Mapper
public interface ApplicationMapper {
    void insert(ApplicationDTO dto);

    ApplicationDTO findByRecruitmentAndApplicant(@Param("recruitmentId") int recruitmentId,
                                                 @Param("applicantEmail") String applicantEmail);

    ApplicationDTO findById(@Param("applicationId") int applicationId);

    List<ApplicationDTO> findCompanyList(@Param("recruitmentId") int recruitmentId,
                                         @Param("pageRequest") PageRequest pageRequest);

    long countCompanyList(@Param("recruitmentId") int recruitmentId);

    ApplicationDTO findCompanyDetail(@Param("recruitmentId") int recruitmentId,
                                     @Param("applicationId") int applicationId);

    int markViewedIfAbsent(@Param("applicationId") int applicationId);

    int updateIntro(@Param("applicationId") int applicationId,
                    @Param("intro") String intro);

    int cancel(@Param("applicationId") int applicationId);

    int reactivateCancelled(@Param("applicationId") int applicationId,
                            @Param("intro") String intro);

    int updateStatusIfPending(@Param("applicationId") int applicationId,
                              @Param("status") ApplicationStatus status);

    int attachContract(@Param("applicationId") int applicationId,
                       @Param("contractId") int contractId);
}
