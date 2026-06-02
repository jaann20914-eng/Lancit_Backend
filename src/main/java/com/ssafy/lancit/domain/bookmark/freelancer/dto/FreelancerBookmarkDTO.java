package com.ssafy.lancit.domain.bookmark.freelancer.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FreelancerBookmarkDTO {
    private Long id;
    private String freelancerEmail;
    private int recruitmentId;
    private LocalDateTime bookmarkedAt;
}