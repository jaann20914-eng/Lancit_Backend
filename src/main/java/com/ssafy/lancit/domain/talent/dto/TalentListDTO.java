package com.ssafy.lancit.domain.talent.dto;

import java.util.ArrayList;
import java.util.List;

import com.ssafy.lancit.global.enums.JobCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentListDTO {
    private String freelancerEmail;
    private String name;
    private JobCategory jobCategory;
    private Integer profileFileId;
    private String shortIntro;
    private List<String> techStacks;
    private Integer viewCount;
    private Boolean isFavorite;
    private Integer publicProjectCount;

    public List<String> getTechStacks() {
        if (techStacks == null) {
            techStacks = new ArrayList<>();
        }
        return techStacks;
    }
}
