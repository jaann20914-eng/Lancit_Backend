package com.ssafy.lancit.domain.bookmark.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
@Service
@RequiredArgsConstructor
public class BookmarkService {
 
    public Object getList(Object... args) {
        // TODO 영은: getList(companyEmail)
        return null;
    }
 
    public Object create(Object... args) {
        // TODO 영은: create(dto, companyEmail)
        return null;
    }
 
    public Object delete(Object... args) {
        // TODO 영은: delete(bookmarkId, companyEmail)
        return null;
    }
 
    public Object searchFreelancers(Object... args) {
        // TODO 영은: searchFreelancers(name, jobCategory, companyEmail) → User 목록 + 찜 여부
        return null;
    }
}
