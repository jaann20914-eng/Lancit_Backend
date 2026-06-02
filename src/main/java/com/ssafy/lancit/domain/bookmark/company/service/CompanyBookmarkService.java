package com.ssafy.lancit.domain.bookmark.company.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;
import com.ssafy.lancit.domain.bookmark.company.mapper.CompanyBookmarkMapper;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 회사가 프리랜서 찜 (bookmark 테이블)
@Service
@RequiredArgsConstructor
public class CompanyBookmarkService {

    private final CompanyBookmarkMapper bookmarkMapper;
    private final UserMapper userMapper; // 프리랜서 검색용

    // CLI-SEAR-01 찜한 프리랜서 목록 조회 (페이지네이션)
    public PageResponse<CompanyBookmarkDTO> getList(String companyEmail, PageRequest pageRequest) {
        // TODO 지원 [1]: List<CompanyBookmarkDTO> list = bookmarkMapper.findByCompany(companyEmail, pageRequest)
        // TODO 지원 [2]: long total = bookmarkMapper.countByCompany(companyEmail)
        // TODO 지원 [3]: return PageResponse.of(list, total, pageRequest)
        List<CompanyBookmarkDTO> list = bookmarkMapper.findByCompany(companyEmail, pageRequest);
        long total = bookmarkMapper.countByCompany(companyEmail);
        return PageResponse.of(list, total, pageRequest);
    }

    // CLI-SEAR-01 직접 찜 추가 - 중복 찜 방지
    @Transactional
    public void create(CompanyBookmarkDTO dto, String companyEmail) {
        // TODO 지원 [1]: 중복 확인
        //               bookmarkMapper.exists(companyEmail, dto.getFreelancerEmail())
        //               true 이면 throw new CustomException(ErrorCode.INVALID_INPUT) or 무시
        // TODO 지원 [2]: dto.setCompanyEmail(companyEmail)
        // TODO 지원 [3]: bookmarkMapper.insert(dto)
    }

    // 찜 취소 - 본인 찜만 삭제 가능
    @Transactional
    public void delete(int bookmarkId, String companyEmail) {
        // TODO 지원 [1]: CompanyBookmarkDTO dto = bookmarkMapper.findById(bookmarkId)
        //               null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [2]: dto.getCompanyEmail() 과 companyEmail 다르면
        //               throw new CustomException(ErrorCode.FORBIDDEN)
        // TODO 지원 [3]: bookmarkMapper.delete(bookmarkId)
    }

    // CLI-SEAR-01 프리랜서 검색 (이름/업종 필터 + 찜 여부 포함, 페이지네이션)
    public PageResponse<UserDTO> searchFreelancers(String name, String jobCategory,
                                                    String companyEmail, PageRequest pageRequest) {
        // TODO 지원 [1]: List<UserDTO> list = userMapper.searchFreelancers(name, jobCategory, pageRequest)
        //               → UserMapper 에 searchFreelancers() 추가 필요
        // TODO 지원 [2]: 각 UserDTO 에 찜 여부 세팅
        //               list.forEach(user -> user.setBookmarked(
        //                   bookmarkMapper.exists(companyEmail, user.getEmail())))
        //               → UserDTO 에 isBookmarked 필드 추가 필요
        // TODO 지원 [3]: long total = userMapper.countFreelancers(name, jobCategory)
        // TODO 지원 [4]: return PageResponse.of(list, total, pageRequest)
        return null;
    }
}