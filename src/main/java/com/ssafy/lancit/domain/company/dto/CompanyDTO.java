package com.ssafy.lancit.domain.company.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.lancit.global.enums.JobCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private boolean isDeleted;

    public CompanyDTO() {}

    @Builder
    @JsonCreator
    public CompanyDTO(
            @JsonProperty("email") String email,
            @JsonProperty("password") String password,
            @JsonProperty("name") String name,
            @JsonProperty("companyName") String companyName,
            @JsonProperty("phone") String phone,
            @JsonProperty("jobCategory") JobCategory jobCategory,
            @JsonProperty("pushable") boolean pushable,
            @JsonProperty("businessNumber") String businessNumber,
            @JsonProperty("businessNumberVerified") boolean businessNumberVerified,
            @JsonProperty("profileFileId") Integer profileFileId
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.companyName = companyName;
        this.phone = phone;
        this.jobCategory = jobCategory;
        this.pushable = pushable;
        this.businessNumber = businessNumber;
        this.businessNumberVerified = businessNumberVerified;
        this.profileFileId = profileFileId;
        this.isDeleted = false;
    }
}