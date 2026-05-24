package com.ssafy.lancit.domain.recruitment.post.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.global.enums.JobCategory;

@Mapper
public interface RecruitmentMapper {
    List<RecruitmentDTO> findAll(@Param("jobCategory") JobCategory jobCategory, @Param("keyword") String keyword);
    List<RecruitmentDTO> findByCompany(@Param("email") String email, @Param("status") String status);
    RecruitmentDTO findById(int recruitmentId);
    void insert(RecruitmentDTO dto);
}
