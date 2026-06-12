package com.ssafy.lancit.domain.portfolio.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioSearchCondition;

@Mapper
public interface PortfolioMapper {
    List<PortfolioDTO> findByEmail(@Param("email") String email,
                                   @Param("pageRequest") PageRequest pageRequest,
                                   @Param("condition") PortfolioSearchCondition condition);
    long countByEmail(@Param("email") String email,
                      @Param("condition") PortfolioSearchCondition condition);

    List<PortfolioDTO> findPublicByEmail(@Param("email") String email,
                                         @Param("pageRequest") PageRequest pageRequest,
                                         @Param("condition") PortfolioSearchCondition condition);
    long countPublicByEmail(@Param("email") String email,
                            @Param("condition") PortfolioSearchCondition condition);

    List<PortfolioDTO> findPublicProjectsByEmail(@Param("email") String email);

    PortfolioDTO findById(@Param("portfolioId") int portfolioId);
    String findOwnerEmailById(@Param("portfolioId") int portfolioId);
    void insert(PortfolioDTO dto);
    int update(@Param("portfolioId") int portfolioId, @Param("dto") PortfolioDTO dto);
    int softDelete(@Param("portfolioId") int portfolioId);
}
