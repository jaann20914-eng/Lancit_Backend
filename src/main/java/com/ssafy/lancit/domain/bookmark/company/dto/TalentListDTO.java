package com.ssafy.lancit.domain.bookmark.company.dto;
import java.time.LocalDateTime;
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
    // user 테이블
    private String email;
    private String name;
    private boolean bookmarked;
    private LocalDateTime createdAt;
    private String profileImageUrl; // 프론트에서 별도 처리

    // portfolio_profile 테이블
    private String displayName;         // display_name
    private JobCategory jobCategory;    // portfolio_profile.job_category (user 대신)
    private Integer profileFileId;      // portfolio_profile.profile_file_id
    private Boolean isPortfolioPublic;  // is_portfolio_public
    private String shortIntro;          // short_intro
    private String description;         // description
}