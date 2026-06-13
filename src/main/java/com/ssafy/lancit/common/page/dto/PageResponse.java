package com.ssafy.lancit.common.page.dto;


import lombok.Getter;

import java.util.List;

/**
 * 페이지네이션 응답 공통 래퍼 : 서버가 프론트한테 보내는 것 (원래 보내려고 했던 데이터(List<T> content) + 페이지 정보 = 묶어서 PageResponse 형태로 묶음)
 *
 * 사용 예) 프론트에서는 아래모양의 응답을 기대하게 되는 꼴
	{
	  "content": [...],
	  "page": 1,
	  "size": 10,
	  "totalElements": 35,
	  "totalPages": 4,
	  "hasNext": true,
	  "hasPrev": false
	}
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;       // 현재 페이지 데이터 : 포트폴리오dto 리스트 or 포트포리오 dto  or ...
    private final int page;              // 현재 위치한 페이지
    private final int size;              // 페이지당 항목 수 
    private final long totalElements;    // 전체 항목 수 
    private final int totalPages;        // 전체 페이지 수
    private final boolean hasNext;       // 다음 페이지 존재 여부
    private final boolean hasPrev;       // 이전 페이지 존재 여부

    private PageResponse(List<T> content, long totalElements, PageRequest req) {
        this.content       = content;
        this.page          = req.getPage();
        this.size          = req.getSize();
        this.totalElements = totalElements;
        this.totalPages    = (int) Math.ceil((double) totalElements / req.getSize());
        this.hasNext       = req.getPage() <= 0
                ? req.getPage() + 1 < this.totalPages
                : req.getPage() < this.totalPages;
        this.hasPrev       = req.getPage() <= 0
                ? req.getPage() > 0
                : req.getPage() > 1;
    }

    
    
    //외부에서 생성자로 접근 불가하게 막고, 아래 함수 통해서만 접근해야함!! (정적 팩토리 메서드)
    public static <T> PageResponse<T> of(List<T> content, long totalElements, PageRequest req) {
    	
    	// 유효성검사 부터
    	if (content == null) content = List.of();  // null 이면 빈 리스트로
        if (totalElements < 0) totalElements = 0;  // 음수 방지
    	
        return new PageResponse<>(content, totalElements, req);
    }
}
