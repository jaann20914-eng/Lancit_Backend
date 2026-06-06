package com.ssafy.lancit.domain.file.dto;

import java.time.LocalDateTime;

import com.ssafy.lancit.global.enums.FileParentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 삭제 시도 했을때
// db내용은 삭제되고
// gcs서버 삭제안되고 예외났을때
// 이후에 gcs 애들에 대하여 배치로 재시도 시킬거임  의 dto


@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileDeleteQueue {
	private  int fileId;
	private String uploadPath;
	private int retryCount;
	private LocalDateTime createdAt;
	
}
