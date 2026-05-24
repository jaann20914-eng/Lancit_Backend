package com.ssafy.lancit.domain.bookmark.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.bookmark.dto.BookmarkDTO;

@Mapper
public interface BookmarkMapper {
   List<BookmarkDTO> findByCompany(String companyEmail);
   void insert(BookmarkDTO dto);
   void delete(int bookmarkId);
   boolean exists(@org.apache.ibatis.annotations.Param("companyEmail") String companyEmail, @org.apache.ibatis.annotations.Param("freelancerEmail") String freelancerEmail);
}
