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
    long countByEmail(String email);

    List<PortfolioDTO> findPublicByEmail(@Param("email") String email,
                                         @Param("pageRequest") PageRequest pageRequest);
    long countPublicByEmail(String email);

    PortfolioDTO findById(int portfolioId);
    String findOwnerEmailById(int portfolioId);
    void insert(PortfolioDTO dto);
    void update(@Param("portfolioId") int portfolioId, @Param("dto") PortfolioDTO dto);
    void delete(int portfolioId);
}