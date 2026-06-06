package com.ssafy.lancit.domain.file.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ssafy.lancit.domain.file.dto.FileDeleteQueue;

@Mapper
public interface FileDeleteQueueMapper {
	
	//삭제 실패한것 테이블에 저장해놓기
	void insert(String uploadPath);
	
	
	// 재시도 대상 전체 조회
	List<FileDeleteQueue> findAll();     
	
	
	// 성공 시 큐에서 제거
	void delete(long id);           
	
	
	// 실패 시 재시도 횟수 증가
	void incrementRetryCount(long id);         

	
}
