package com.ssafy.lancit.domain.recruitment.application.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationPortfolioSummaryResponse;

@Mapper
public interface ApplicationPortfolioSnapshotMapper {

    int insertPortfolio(@Param("applicationId") int applicationId,
                        @Param("portfolioId") int portfolioId,
                        @Param("sortOrder") int sortOrder);

    void insertFiles(@Param("applicationId") int applicationId,
                     @Param("portfolioId") int portfolioId);

    void deleteByApplicationId(@Param("applicationId") int applicationId);

    List<ApplicationPortfolioSummaryResponse> findSummariesByApplicationId(
            @Param("applicationId") int applicationId);

    PortfolioDTO findPortfolio(@Param("applicationId") int applicationId,
                               @Param("portfolioId") int portfolioId);

    List<FileDTO> findFiles(@Param("applicationId") int applicationId,
                            @Param("portfolioId") int portfolioId);

    List<Integer> findFileIdsByApplicationId(@Param("applicationId") int applicationId);

    boolean existsFile(@Param("applicationId") int applicationId,
                       @Param("portfolioId") int portfolioId,
                       @Param("fileId") int fileId);

    boolean isFileReferenced(@Param("fileId") int fileId);
}
