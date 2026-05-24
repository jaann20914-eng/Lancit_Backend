package com.ssafy.lancit.domain.file.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.FileParentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileDTO {
    private int fileId;
    private String userEmail;       // 업로더 이메일 - 프리랜서 (null 허용)
    private String companyEmail;    // 업로더 이메일 - 회사 (null 허용)
                                    // 둘 중 하나만 값을 가짐
    private String sysName;
    private String oriName;
    private FileParentType parentType;
    private Integer parentId;       // PROFILE 타입은 null
    private String uploadPath;      // GCS 경로
    private LocalDateTime createdAt;
    private int fileSize;
}