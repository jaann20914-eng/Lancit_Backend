package com.ssafy.lancit.domain.bookmark.company.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.global.enums.JobCategory;

@Mapper
public interface CompanyBookmarkMapper {

    // 찜 목록 조회
    List<CompanyBookmarkDTO> findByCompany(
            @Param("companyEmail") String companyEmail,
            @Param("offset") int offset,
            @Param("size") int size);

    // 찜 전체 개수
    long countByCompany(@Param("companyEmail") String companyEmail);

    // 찜 추가
    void insert(CompanyBookmarkDTO dto);

    // 찜 취소
    void delete(int bookmarkId);

    // 중복 찜 확인
    boolean exists(@Param("companyEmail") String companyEmail,
                   @Param("freelancerEmail") String freelancerEmail);

    // 찜 단건 조회 (소유자 검증용)
    CompanyBookmarkDTO findById(int bookmarkId);
}