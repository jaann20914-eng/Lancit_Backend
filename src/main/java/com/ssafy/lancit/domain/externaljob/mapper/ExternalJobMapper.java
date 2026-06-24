package com.ssafy.lancit.domain.externaljob.mapper;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobDTO;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobSearchCondition;
import com.ssafy.lancit.domain.externaljob.dto.ExternalJobUpsertCommand;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExternalJobMapper {
    int upsertExternalJob(ExternalJobUpsertCommand command);

    List<ExternalJobDTO> findExternalJobs(@Param("condition") ExternalJobSearchCondition condition,
                                          @Param("pageRequest") PageRequest pageRequest);

    long countExternalJobs(@Param("condition") ExternalJobSearchCondition condition);

    ExternalJobDTO findById(@Param("id") Long id);
}
