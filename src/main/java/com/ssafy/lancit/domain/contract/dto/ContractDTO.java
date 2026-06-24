package com.ssafy.lancit.domain.contract.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.ContractStatus;

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
public class ContractDTO {
	private Integer recruitmentId;
    private Integer contractId;

    private String companyEmail;
    private String freelancerEmail;

    private ContractStatus status;
    
    private LocalDateTime createdAt;

}