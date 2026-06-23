package com.ssafy.lancit.common.page.dto;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * 페이지네이션 요청 공통 DTO : 프론트가 서버한테 보내는 것 
 *
 * 요청 예)
 *   GET요청 -- /api/portfolios?page=1&size=10&sort=created_at&direction=DESC 이 들어온다면
 *   portfolioController 가 요청을 받을 때, @ModelAttribute로 한번에 받을 수 있다 (쿼리스트링을 객체로 한 번에 받을 때 사용)
 */

@Getter
@Setter
public class PageRequest {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private int page = 1;               // 현재 페이지 (1부터 시작)  -- 디폴트 값으로 일단 넣어놓기
    private int size = 10;              // 페이지당 항목 수
    private String sort = "created_at"; // 정렬 기준 컬럼 (DB 컬럼명)
    private String direction = "DESC";  // 정렬 방향 (ASC / DESC)

    
    // 몇번째 id~ 몇번째 id 를 뽑아야지 페이지에 맞게 뽑는건지 계산 함
    public int getPage() {
        return page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
    }

    public int getSize() {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public int getOffset() {
        long offset = (long) (getPage() - 1) * getSize();
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
    }
    
    
    // 오름차순 , 내림차순 정하는 함수
    //XML: ORDER BY ${pageRequest.sort} ${pageRequest.safeDirection}
    public String getSafeDirection() {
        if ("ASC".equalsIgnoreCase(direction)) return "ASC"; // 프론트에서 asc 요청이면 asc
        return "DESC"; // 아닌건 전부 desc
    }
    
    
    //order by 칼럼 지정하는 함수
    public String getSafeSort() {
    	// 프론트에서 sort=created_at -> created_at 반환해서 이대로 정렬
    	// 프론트에서 sort=DROP TABLE user -> 허용목록에 없으니 디폴트 값인 created_at 반환해서 sql 인젝션 막기
        return sort != null && ALLOWED_SORT_COLUMNS.contains(sort) ? sort : "created_at";
    }
    
    // TODO 영은: 나중에 정렬 가능하도록 허용할 칼럼들 이곳에 넣으면 됨
    // TODO 지원: 나중에 정렬 가능하도록 허용할 칼럼들 이곳에 넣으면 됨
    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "created_at", "sent_at", "bookmarked_at",
        "contract_written_at", "applied_at", "title","name",
        "updated_at", "budget", "recruitment_end_at"
    );
    
    
}
