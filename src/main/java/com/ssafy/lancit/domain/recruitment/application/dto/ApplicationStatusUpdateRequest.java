package com.ssafy.lancit.domain.recruitment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "지원 상태 변경 요청")
public class ApplicationStatusUpdateRequest {

    @NotBlank
    @Schema(description = "변경할 상태", allowableValues = {"ACCEPTED", "REJECTED"})
    private String status;
}
