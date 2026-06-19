package com.ssafy.lancit.domain.proposal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "프리랜서 제안 생성 요청")
public class ProposalCreateRequest {

    @NotBlank
    @Email
    @Schema(description = "제안을 받을 프리랜서 이메일")
    private String freelancerEmail;

    @NotNull
    @Positive
    @Schema(description = "제안에 사용할 회사 소유 공고 ID")
    private Integer recruitmentId;
}
