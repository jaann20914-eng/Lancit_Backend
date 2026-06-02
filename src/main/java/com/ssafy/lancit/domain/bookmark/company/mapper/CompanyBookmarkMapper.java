package com.ssafy.lancit.domain.bookmark.company.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;

@Mapper
public interface CompanyBookmarkMapper {
    List<CompanyBookmarkDTO> findByCompany(@Param("companyEmail") String companyEmail,
                                    @Param("pageRequest") PageRequest pageRequest);
    long countByCompany(String companyEmail);

    void insert(CompanyBookmarkDTO dto);
    void delete(int bookmarkId);
    boolean exists(@Param("companyEmail") String companyEmail,
                   @Param("freelancerEmail") String freelancerEmail);
    CompanyBookmarkDTO findById(int bookmarkId); // delete() 소유자 검증용
}