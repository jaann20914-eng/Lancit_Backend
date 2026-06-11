package com.ssafy.lancit.domain.portfolio.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;

@Mapper
public interface PortfolioMapper {
    List<PortfolioDTO> findByEmail(@Param("email") String email,
                                   @Param("keyword") String keyword,
                                   @Param("visibility") Boolean visibility,
                                   @Param("category") String category,
                                   @Param("sort") String sort,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    long countByEmail(@Param("email") String email,
                      @Param("keyword") String keyword,
                      @Param("visibility") Boolean visibility,
                      @Param("category") String category);

    List<PortfolioDTO> findPublicByEmail(@Param("email") String email,
                                         @Param("keyword") String keyword,
                                         @Param("category") String category,
                                         @Param("sort") String sort,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    long countPublicByEmail(@Param("email") String email,
                            @Param("keyword") String keyword,
                            @Param("category") String category);

    PortfolioDTO findById(int portfolioId);
    PortfolioDTO findByIdIncludingDeleted(int portfolioId);
    String findOwnerEmailById(int portfolioId);
    int insert(PortfolioDTO dto);
    int update(@Param("portfolioId") int portfolioId, @Param("dto") PortfolioDTO dto);
    int softDelete(int portfolioId);
}
