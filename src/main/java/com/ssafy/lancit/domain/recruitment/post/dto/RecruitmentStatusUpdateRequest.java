package com.ssafy.lancit.domain.recruitment.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "공고 상태 변경 요청")
public class RecruitmentStatusUpdateRequest {
    @NotBlank
    @Schema(description = "변경할 상태. EXPIRED는 저장하지 않습니다.", allowableValues = {"OPEN", "CLOSED", "CANCELLED"})
    private String status;
}
