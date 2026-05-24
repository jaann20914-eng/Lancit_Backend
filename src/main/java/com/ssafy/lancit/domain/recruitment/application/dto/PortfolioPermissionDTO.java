package com.ssafy.lancit.domain.recruitment.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortfolioPermissionDTO {
    private int permissionId;
    private int applicationId;
    private int portfolioId;
    private boolean isPublic;
}
