package com.ssafy.lancit.domain.portfolio.dto;

import java.time.LocalDateTime;
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
public class PortfolioProfileDTO {
    private String freelancerEmail;
    private String name;
    private JobCategory jobCategory;
    private Integer profileFileId;
    private Boolean isPortfolioPublic;
    private String shortIntro;
    private String description;
    private List<String> techStacks;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PortfolioDTO> projects;

    public List<String> getTechStacks() {
        if (techStacks == null) {
            techStacks = new ArrayList<>();
        }
        return techStacks;
    }
}
