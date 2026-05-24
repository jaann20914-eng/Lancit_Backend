package com.ssafy.lancit.domain.bookmark.dto;

import lombok.*;

import java.time.LocalDateTime;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookmarkDTO {
    private int bookmarkId;
    private String companyEmail;
    private String freelancerEmail;
    private Integer applicationId;      // null = 직접 찜
    private LocalDateTime bookmarkedAt;
}
 