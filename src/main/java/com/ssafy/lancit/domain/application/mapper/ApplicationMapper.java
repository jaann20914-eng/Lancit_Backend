package com.ssafy.lancit.domain.application.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.application.dto.ApplicationDTO;

@Mapper
public interface ApplicationMapper {
    void insert(ApplicationDTO dto);
    List<ApplicationDTO> findByRecruitment(int recruitmentId);
    ApplicationDTO findById(int applicationId);
    void updateBookmark(@Param("applicationId") int applicationId, @Param("bookmarked") boolean bookmarked);
}