package com.ssafy.lancit.domain.portfolio.dto;

import com.ssafy.lancit.common.page.dto.PageRequest;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PortfolioSearchCondition {

    private String keyword;
    private String visibility;
    private String category;
    private String sort = "latest";
    private int page = 1;
    private int size = 10;

    public int getOffset() {
        return (getSafePage() - 1) * getSafeSize();
    }

    public int getSafePage() {
        return page < 1 ? 1 : page;
    }

    public int getSafeSize() {
        if (size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    public String getSafeSort() {
        return "oldest".equalsIgnoreCase(sort) ? "oldest" : "latest";
    }

    public PageRequest toPageRequest() {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(getSafePage());
        pageRequest.setSize(getSafeSize());
        return pageRequest;
    }
}
