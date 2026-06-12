package com.ssafy.lancit.domain.portfolio.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.portfolio.dto.PortfolioProfileDTO;

@Mapper
public interface PortfolioProfileMapper {

    PortfolioProfileDTO findByFreelancerEmail(@Param("freelancerEmail") String freelancerEmail);

    boolean existsProfile(@Param("freelancerEmail") String freelancerEmail);

    Boolean isPortfolioPublic(@Param("freelancerEmail") String freelancerEmail);

    void insertProfile(PortfolioProfileDTO dto);

    int updateProfile(PortfolioProfileDTO dto);

    List<String> findTechStacks(@Param("freelancerEmail") String freelancerEmail);

    void deleteTechStacks(@Param("freelancerEmail") String freelancerEmail);

    void insertTechStack(@Param("freelancerEmail") String freelancerEmail,
                         @Param("techStack") String techStack);

    void incrementViewCount(@Param("freelancerEmail") String freelancerEmail);
}
