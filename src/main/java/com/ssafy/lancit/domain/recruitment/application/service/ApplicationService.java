package com.ssafy.lancit.domain.recruitment.application.service;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.bookmark.company.mapper.CompanyBookmarkMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.recruitment.application.dto.ApplicationDTO;
import com.ssafy.lancit.domain.recruitment.application.dto.PortfolioPermissionDTO;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.PortfolioPermissionMapper;
import com.ssafy.lancit.global.enums.FileParentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 공고문 지원 관련 서비스
// Redis, STOMP 직접 관련 없음
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final PortfolioPermissionMapper portfolioPermissionMapper;
    private final CompanyBookmarkMapper bookmarkMapper;
    private final PortfolioMapper portfolioMapper;
    private final FileService fileService; // 포트폴리오 결과물 파일 조회용

    // APPLY-03 공고문 지원
    // recruitment_application INSERT + portfolio_permission INSERT (트랜잭션)
    @Transactional
    public void apply(int recruitmentId, ApplicationDTO dto, String email) {
        // TODO 영은 [1]: dto.setRecruitmentId(recruitmentId)
        // TODO 영은 [2]: dto.setApplicantEmail(email)
        // TODO 영은 [3]: applicationMapper.insert(dto) → applicationId 생성
        // TODO 영은 [4]: dto.getPortfolioIds() 순회하며 portfolio_permission INSERT
        //               PortfolioPermissionDTO permission = new PortfolioPermissionDTO()
        //               permission.setApplicationId(dto.getApplicationId())
        //               permission.setPortfolioId(portfolioId)
        //               portfolioPermissionMapper.insert(permission)
    }

    // CLI-APPLY-03 지원자 목록 조회 (페이지네이션)
    public PageResponse<ApplicationDTO> getList(int recruitmentId, String companyEmail,
                                                 PageRequest pageRequest) {
        // TODO 영은 [1]: List<ApplicationDTO> list = applicationMapper.findByRecruitment(recruitmentId, pageRequest)
        // TODO 영은 [2]: long total = applicationMapper.countByRecruitment(recruitmentId)
        // TODO 영은 [3]: return PageResponse.of(list, total, pageRequest)
        List<ApplicationDTO> list = applicationMapper.findByRecruitment(recruitmentId, pageRequest);
        long total = applicationMapper.countByRecruitment(recruitmentId);
        return PageResponse.of(list, total, pageRequest);
    }

    // CLI-APPLY-04 지원자 포트폴리오 조회 (열람 허용된 것만, 페이지네이션)
    // bannerFileId + 결과물 파일 포함하여 Map 으로 반환
    // → 컨트롤러 반환 타입: PageResponse<Map<String, Object>>
    public PageResponse<Map<String, Object>> getPermittedPortfolios(int applicationId,
                                                                     PageRequest pageRequest) {
        // TODO 영은 [1]: portfolio_permission 에서 applicationId 기준 허용된 portfolioId 목록 조회
        //               List<PortfolioPermissionDTO> permissions = portfolioPermissionMapper.findByApplication(applicationId)
        // TODO 영은 [2]: 각 portfolioId 로 portfolioMapper.findById() + fileService.findByParent() 조회
        //               Map 으로 조립 { "portfolio": dto, "files": fileList }
        // TODO 영은 [3]: PageResponse 로 감싸서 반환
        return null;
    }

    // CLI-APPLY-03 지원자 찜 토글 - is_bookmarked_by_company 토글
    @Transactional
    public void toggleBookmark(int applicationId, String companyEmail) {
        // TODO 영은 [1]: ApplicationDTO dto = applicationMapper.findById(applicationId)
        // TODO 영은 [2]: boolean current = dto.isBookmarkedByCompany()
        // TODO 영은 [3]: applicationMapper.updateBookmark(applicationId, !current)
    }
}