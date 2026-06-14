package com.ssafy.lancit.domain.contract.dto;

import java.time.LocalDateTime;

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
public class ContractCancelRequestDTO {

    private Integer contractId;

    private String requesterEmail;

    private LocalDateTime requestedAt;
}
