package com.ssafy.lancit.domain.company.dto;

import com.ssafy.lancit.global.enums.JobCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyDTO {
    private String email;
    private String password;
    private String name;
    private String companyName;
    private String phone;
    private JobCategory jobCategory;
    private boolean pushable;
    private String businessNumber;
    private boolean businessNumberVerified;
    private Integer profileFileId;
}
 
