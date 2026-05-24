package com.ssafy.lancit.domain.portfolio.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioService {
 
    public Object getMyList(Object... args) {
        // TODO 영은: getMyList(email)
        return null;
    }
 
    public Object getPublicList(Object... args) {
        // TODO 영은: getPublicList(email)
        return null;
    }
 
    public Object getOne(Object... args) {
        // TODO 영은: getOne(portfolioId)
        return null;
    }
 
    public Object create(Object... args) {
        // TODO 영은: create(dto, email) → FileService.upload()
        return null;
    }
 
    public Object update(Object... args) {
        // TODO 영은: update(portfolioId, dto)
        return null;
    }
 
    public Object delete(Object... args) {
        // TODO 영은: delete(portfolioId) → FileService.deleteByParent() + portfolio 삭제
        return null;
    }
}