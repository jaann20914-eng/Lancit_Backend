package com.ssafy.lancit.domain.portfolio.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;

@Mapper
public interface PortfolioMapper {
    List<PortfolioDTO> findByEmail(@Param("email") String email,
                                   @Param("pageRequest") PageRequest pageRequest);
    long countByEmail(@Param("email") String email);

    List<PortfolioDTO> findPublicByEmail(@Param("email") String email,
                                         @Param("pageRequest") PageRequest pageRequest);
    long countPublicByEmail(@Param("email") String email);

    PortfolioDTO findById(@Param("portfolioId") int portfolioId);
    String findOwnerEmailById(@Param("portfolioId") int portfolioId);
    void insert(PortfolioDTO dto);
    int update(@Param("portfolioId") int portfolioId, @Param("dto") PortfolioDTO dto);
    int softDelete(@Param("portfolioId") int portfolioId);
}
