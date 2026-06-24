package com.ssafy.lancit.domain.user.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.lancit.global.enums.JobCategory;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    private String email;
    private String password;
    private String name;
    private String phone;
    private JobCategory jobCategory;
    private boolean pushable;
    private Integer profileFileId;
    private boolean isBookmarked;
    private boolean isDeleted;
    private LocalDateTime createdAt;

    public UserDTO() {}

    @Builder
    @JsonCreator
    public UserDTO(
            @JsonProperty("email") String email,
            @JsonProperty("password") String password,
            @JsonProperty("name") String name,
            @JsonProperty("phone") String phone,
            @JsonProperty("jobCategory") JobCategory jobCategory,
            @JsonProperty("pushable") boolean pushable,
            @JsonProperty("profileFileId") Integer profileFileId,
            @JsonProperty("createdAt") LocalDateTime createdAt
    ) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.jobCategory = jobCategory;
        this.pushable = pushable;
        this.profileFileId = profileFileId;
        this.isBookmarked = false;
        this.isDeleted = false;
        this.createdAt=createdAt;
    }
}