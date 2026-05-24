package com.ssafy.lancit.domain.proposal.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.ProposalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProposalDTO {
    private int proposalId;
    private String companyEmail;
    private String freelancerEmail;
    private String title;
    private String content;
    private ProposalStatus status;
    private LocalDateTime sentAt;
}
 