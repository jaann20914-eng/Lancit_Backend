package com.ssafy.lancit.domain.recruitment.application.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApplicationService {
 
    public Object apply(Object... args) {
        // TODO 영은: apply(recruitmentId, dto, email) → Application + PortfolioPermission 삽입
        return null;
    }
 
    public Object getList(Object... args) {
        // TODO 영은: getList(recruitmentId, companyEmail)
        return null;
    }
 
    public Object getPermittedPortfolios(Object... args) {
        // TODO 영은: getPermittedPortfolios(applicationId) → PortfolioPermission → Portfolio + File 조회
        return null;
    }
 
    public Object toggleBookmark(Object... args) {
        // TODO 영은: toggleBookmark(applicationId, companyEmail) → Bookmark 삽입/삭제 + isBookmarkedByCompany 수정
        return null;
    }
}
