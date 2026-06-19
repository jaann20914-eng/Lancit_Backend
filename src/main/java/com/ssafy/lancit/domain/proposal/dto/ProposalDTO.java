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
    private Integer proposalId;
    private String companyEmail;
    private String freelancerEmail;
    private Integer recruitmentId;
    private String recruitmentTitle;
    private Integer budget;
    private String workLocation;
    private LocalDateTime contractStartAt;
    private LocalDateTime contractEndAt;
    private ProposalStatus status;
    private LocalDateTime sentAt;
    private Integer contractId;
    private Integer chatRoomId;
}
