package com.ssafy.lancit.domain.externaljob.mapper;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobCollectionLogCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUserRecommendationCommand;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import com.ssafy.lancit.global.enums.ExternalFreelanceType;
import com.ssafy.lancit.global.enums.ExternalJobRecommendationType;
import com.ssafy.lancit.global.enums.ExternalJobSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ExternalJobMapper {
    int upsertExternalJob(ExternalJobUpsertCommand command);

    List<ExternalJobDTO> findExternalJobs(@Param("condition") ExternalJobSearchCondition condition,
                                          @Param("pageRequest") PageRequest pageRequest);

    long countExternalJobs(@Param("condition") ExternalJobSearchCondition condition);

    List<ExternalJobDTO> findVisibleExternalJobsForRecommendation();

    List<ExternalJobDTO> findExternalJobsForReclassification(@Param("source") ExternalJobSource source);

    int upsertExternalJobUserRecommendation(ExternalJobUserRecommendationCommand command);

    int updateExternalJobClassification(@Param("id") Long id,
                                        @Param("freelanceType") ExternalFreelanceType freelanceType,
                                        @Param("recommendationType") ExternalJobRecommendationType recommendationType,
                                        @Param("recommendationScore") Integer recommendationScore,
                                        @Param("visible") Boolean visible,
                                        @Param("visibilityReason") String visibilityReason,
                                        @Param("updatedAt") LocalDateTime updatedAt);

    ExternalJobDTO findById(@Param("id") Long id);

    int refreshVisibilityByPolicy(@Param("source") ExternalJobSource source,
                                  @Param("now") LocalDateTime now,
                                  @Param("staleBefore") LocalDateTime staleBefore);

    int insertCollectionLog(ExternalJobCollectionLogCommand command);

    int tryAcquireCollectionLock(@Param("source") ExternalJobSource source,
                                 @Param("lockedBy") String lockedBy,
                                 @Param("lockedAt") LocalDateTime lockedAt,
                                 @Param("lockedUntil") LocalDateTime lockedUntil,
                                 @Param("updatedAt") LocalDateTime updatedAt);

    int releaseCollectionLock(@Param("source") ExternalJobSource source,
                              @Param("lockedBy") String lockedBy,
                              @Param("releasedAt") LocalDateTime releasedAt);
}
