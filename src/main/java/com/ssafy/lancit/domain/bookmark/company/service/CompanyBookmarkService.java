package com.ssafy.lancit.domain.bookmark.company.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;
import com.ssafy.lancit.domain.bookmark.company.dto.TalentListDTO;
import com.ssafy.lancit.domain.bookmark.company.mapper.CompanyBookmarkMapper;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import com.ssafy.lancit.global.enums.JobCategory;

import lombok.RequiredArgsConstructor;

// 회사가 프리랜서 찜 (bookmark 테이블)
@Service
@RequiredArgsConstructor
public class CompanyBookmarkService {

    private final CompanyBookmarkMapper companyBookmarkMapper;
    private final UserMapper userMapper; // 프리랜서 검색용
    private final CompanyMapper companyMapper;
    private final FileService fileService;

    // CLI-SEAR-01 찜한 프리랜서 목록 조회 (페이지네이션)
//    public PageResponse<CompanyBookmarkDTO> getList(String companyEmail, PageRequest pageRequest) {
//
//        // 찜 목록 조회 (bookmarked_at DESC 정렬)
//        List<CompanyBookmarkDTO> list = companyBookmarkMapper.findByCompany(
//                companyEmail, pageRequest.getOffset(), pageRequest.getSize());
//
//        // 전체 개수 (페이지 계산용)
//        long total = companyBookmarkMapper.countByCompany(companyEmail);
//
//        return PageResponse.of(list, total, pageRequest);
//    }

    // CLI-SEAR-01 직접 찜 추가 - 중복 찜 방지
    @Transactional
    public void create(CompanyBookmarkDTO dto, String companyEmail) {
        if (companyBookmarkMapper.exists(companyEmail, dto.getFreelancerEmail())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        dto.setCompanyEmail(companyEmail);
        companyBookmarkMapper.insert(dto);
    }

    // 찜 취소 - 본인 찜만 삭제 가능
//    @Transactional
//    public void delete(int bookmarkId, String companyEmail) {
//        CompanyBookmarkDTO dto = companyBookmarkMapper.findById(bookmarkId);
//        if (dto == null) throw new CustomException(ErrorCode.NOT_FOUND);
//        if (!dto.getCompanyEmail().equals(companyEmail)) {
//            throw new CustomException(ErrorCode.FORBIDDEN);
//        }
//        companyBookmarkMapper.delete(bookmarkId);
//    }

    // 프리랜서 목록 조회
//    public PageResponse<UserDTO> searchFreelancers(
//            String companyEmail,
//            String keyword,
//            JobCategory jobCategory,
//            boolean bookmarked,
//            PageRequest pageRequest) {
//
//    	List<UserDTO> list = userMapper.searchFreelancers(
//    	        keyword, jobCategory, bookmarked, companyEmail,
//    	        pageRequest.getSafeSort(),
//    	        pageRequest.getSafeDirection(),
//    	        pageRequest.getOffset(),
//    	        pageRequest.getSize());
//
//        list.forEach(user ->
//                user.setBookmarked(
//                        companyBookmarkMapper.exists(companyEmail, user.getEmail())));
//
//        long total = userMapper.countFreelancers(keyword, jobCategory, bookmarked, companyEmail);
//
//        return PageResponse.of(list, total, pageRequest);
//    }
    
    //북마크제거
    @Transactional
    public void deleteByFreelancer(String companyEmail, String freelancerEmail) {
        CompanyBookmarkDTO dto = companyBookmarkMapper.findByCompanyAndFreelancer(companyEmail, freelancerEmail);
        if (dto == null) throw new CustomException(ErrorCode.NOT_FOUND);
        companyBookmarkMapper.delete(dto.getBookmarkId());
    }
    
    
    
    public PageResponse<TalentListDTO> searchTalents(
            String companyEmail,
            String keyword,
            JobCategory jobCategory,
            boolean bookmarked,
            PageRequest pageRequest) {

    	List<TalentListDTO> list =
    	        companyBookmarkMapper.searchTalents(
    	                keyword,
    	                jobCategory,
    	                bookmarked,
    	                companyEmail,
    	                pageRequest.getSafeSort(),
    	                pageRequest.getSafeDirection(),
    	                pageRequest.getOffset(),
    	                pageRequest.getSize());

    	for (TalentListDTO dto : list) {

    	    if (dto.getProfileFileId() != null) {

    	        String signedUrl =
    	                fileService.getSignedUrl(dto.getProfileFileId());

    	        dto.setProfileImageUrl(signedUrl);
    	    }
    	}

        long total =
                companyBookmarkMapper.countTalents(
                        keyword,
                        jobCategory,
                        bookmarked,
                        companyEmail);

        return PageResponse.of(list, total, pageRequest);
    }
    
}