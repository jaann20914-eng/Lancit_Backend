package com.ssafy.lancit.domain.contract.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.ContractFileType;

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
public class ContractFileDTO {

    private Integer contractFileId;

    private Integer contractId;

    private Integer fileId;

    private ContractFileType type;

    private LocalDateTime createdAt;
    
    private String uploaderEmail;
}