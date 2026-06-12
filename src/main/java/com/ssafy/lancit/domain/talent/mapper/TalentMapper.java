package com.ssafy.lancit.domain.talent.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.talent.dto.TalentDetailDTO;
import com.ssafy.lancit.domain.talent.dto.TalentListDTO;
import com.ssafy.lancit.domain.talent.dto.TalentSearchCondition;

@Mapper
public interface TalentMapper {

    List<TalentListDTO> findTalents(@Param("companyEmail") String companyEmail,
                                    @Param("condition") TalentSearchCondition condition);

    long countTalents(@Param("companyEmail") String companyEmail,
                      @Param("condition") TalentSearchCondition condition);

    TalentDetailDTO findTalentDetail(@Param("companyEmail") String companyEmail,
                                     @Param("freelancerEmail") String freelancerEmail);

    void incrementViewCount(@Param("freelancerEmail") String freelancerEmail);
}
