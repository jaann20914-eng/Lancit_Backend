package com.ssafy.lancit.domain.calendar.category.dto;

import com.ssafy.lancit.global.enums.OwnerType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryDTO {
    private int categoryId;
    private String email;           // 소유자 이메일
    private OwnerType ownerType;    // USER | COMPANY
    private String categoryName;
    private String color;           // hex 코드 (#FF5733)
}