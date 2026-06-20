package com.ssafy.lancit.domain.recruitment.application.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.recruitment.application.dto.PortfolioPermissionDTO;

@Mapper
public interface PortfolioPermissionMapper {
    void insert(PortfolioPermissionDTO dto);

    void insertAll(@Param("applicationId") int applicationId,
                   @Param("portfolioIds") List<Integer> portfolioIds);

    void deleteByApplicationId(@Param("applicationId") int applicationId);

    List<PortfolioPermissionDTO> findByApplication(int applicationId);

    List<Integer> findPortfolioIdsByApplicationId(@Param("applicationId") int applicationId);

    boolean existsCompanyPermission(@Param("applicationId") int applicationId,
                                    @Param("portfolioId") int portfolioId,
                                    @Param("recruitmentId") int recruitmentId,
                                    @Param("companyEmail") String companyEmail);
}
 
