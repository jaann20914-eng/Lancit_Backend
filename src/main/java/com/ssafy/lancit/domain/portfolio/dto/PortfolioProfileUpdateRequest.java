package com.ssafy.lancit.domain.portfolio.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PortfolioProfileUpdateRequest {
    private Boolean isPortfolioPublic;
    private String intro;
    private List<String> techStacks;
}
