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
    private int recruitmentId;   // 어떤 공고로 제안하는지
    private int contractId;      // 응답: 생성된 계약서 ID
    private int chatRoomId;      // 응답: 생성된 채팅방 ID (프론트 이동용)
}