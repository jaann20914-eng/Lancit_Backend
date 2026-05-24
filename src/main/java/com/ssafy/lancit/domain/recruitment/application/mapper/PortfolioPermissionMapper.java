package com.ssafy.lancit.domain.recruitment.application.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.recruitment.application.dto.PortfolioPermissionDTO;

@Mapper
public interface PortfolioPermissionMapper {
    void insert(PortfolioPermissionDTO dto);
    List<PortfolioPermissionDTO> findByApplication(int applicationId);
}
 
