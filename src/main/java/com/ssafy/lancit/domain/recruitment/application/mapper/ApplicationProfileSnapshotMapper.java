package com.ssafy.lancit.domain.recruitment.application.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationProfileSnapshotDTO;

@Mapper
public interface ApplicationProfileSnapshotMapper {
    void insert(ApplicationProfileSnapshotDTO snapshot);

    void insertTechStack(@Param("applicationId") int applicationId,
                         @Param("techStack") String techStack,
                         @Param("sortOrder") int sortOrder);

    ApplicationProfileSnapshotDTO findByApplicationId(@Param("applicationId") int applicationId);

    List<String> findTechStacksByApplicationId(@Param("applicationId") int applicationId);

    boolean isProfileFileReferenced(@Param("fileId") int fileId);
}
