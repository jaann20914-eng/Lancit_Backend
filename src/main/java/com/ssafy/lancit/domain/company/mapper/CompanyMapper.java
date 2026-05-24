package com.ssafy.lancit.domain.company.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.company.dto.CompanyDTO;

@Mapper
public interface CompanyMapper {
    CompanyDTO findByEmail(String email);
    void insert(CompanyDTO dto);
    void update(CompanyDTO dto);
    void delete(String email);
    boolean existsByEmail(String email);
}
