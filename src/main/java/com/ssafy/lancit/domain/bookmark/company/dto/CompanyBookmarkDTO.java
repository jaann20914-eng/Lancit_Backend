package com.ssafy.lancit.domain.bookmark.company.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyBookmarkDTO {
    private Integer bookmarkId;
    private String companyEmail;
    private String freelancerEmail;
    private Integer applicationId;      // null = 직접 찜
    private LocalDateTime bookmarkedAt;
}
 