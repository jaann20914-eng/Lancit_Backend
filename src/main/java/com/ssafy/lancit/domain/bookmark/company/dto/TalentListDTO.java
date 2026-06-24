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

    private String email;
    private String name;

    private JobCategory jobCategory;

    private Integer profileFileId;
    private String profileImageUrl;

    private boolean bookmarked;
    
    private LocalDateTime createdAt;

}