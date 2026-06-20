package com.ssafy.lancit.domain.portfolio.dto;

import java.util.List;

import com.ssafy.lancit.global.enums.JobCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PortfolioProfileUpdateRequest {
    @NotBlank
    @Size(max = 100)
    private String displayName;

    @NotNull
    private JobCategory jobCategory;

    private Integer profileFileId;

    private Boolean isPortfolioPublic;

    @Size(max = 30)
    private String intro;

    @Size(max = 200)
    private String description;

    private List<@Size(max = 100) String> techStacks;
}
