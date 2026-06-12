package com.ssafy.lancit.domain.talent.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;
import com.ssafy.lancit.domain.bookmark.company.mapper.CompanyBookmarkMapper;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioProfileMapper;
import com.ssafy.lancit.domain.talent.dto.TalentDetailDTO;
import com.ssafy.lancit.domain.talent.dto.TalentListDTO;
import com.ssafy.lancit.domain.talent.dto.TalentSearchCondition;
import com.ssafy.lancit.domain.talent.mapper.TalentMapper;
import com.ssafy.lancit.global.enums.JobCategory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TalentService {

    private static final String ROLE_COMPANY = "COMPANY";
    private static final String DEFAULT_SORT = "VIEW_COUNT";
    private static final String SORT_VIEW_COUNT = "VIEW_COUNT";
    private static final String SORT_NAME = "NAME";

    private final TalentMapper talentMapper;
    private final PortfolioMapper portfolioMapper;
    private final PortfolioProfileMapper portfolioProfileMapper;
    private final CompanyBookmarkMapper companyBookmarkMapper;

    public PageResponse<TalentListDTO> getTalents(TalentSearchCondition condition) {
        String companyEmail = requireCompany();
        TalentSearchCondition normalized = normalizeCondition(condition);

        List<TalentListDTO> list = talentMapper.findTalents(companyEmail, normalized);
        list.forEach(this::attachTechStacks);
        long total = talentMapper.countTalents(companyEmail, normalized);

        return PageResponse.of(list, total, normalized.toPageRequest());
    }

    @Transactional
    public TalentDetailDTO getTalentDetail(String freelancerEmail) {
        String companyEmail = requireCompany();
        TalentDetailDTO beforeIncrease = talentMapper.findTalentDetail(companyEmail, freelancerEmail);
        if (beforeIncrease == null) {
            throw new CustomException(ErrorCode.TALENT_NOT_FOUND);
        }

        talentMapper.incrementViewCount(freelancerEmail);
        TalentDetailDTO detail = talentMapper.findTalentDetail(companyEmail, freelancerEmail);
        attachTechStacks(detail);
        detail.setProjects(portfolioMapper.findPublicProjectsByEmail(freelancerEmail));
        return detail;
    }

    @Transactional
    public void favorite(String freelancerEmail) {
        String companyEmail = requireCompany();
        TalentDetailDTO talent = talentMapper.findTalentDetail(companyEmail, freelancerEmail);
        if (talent == null) {
            throw new CustomException(ErrorCode.TALENT_NOT_FOUND);
        }

        if (companyBookmarkMapper.existsByCompanyEmailAndFreelancerEmail(companyEmail, freelancerEmail)) {
            return;
        }

        CompanyBookmarkDTO dto = CompanyBookmarkDTO.builder()
                .companyEmail(companyEmail)
                .freelancerEmail(freelancerEmail)
                .applicationId(null)
                .build();
        companyBookmarkMapper.insert(dto);
    }

    @Transactional
    public void unfavorite(String freelancerEmail) {
        String companyEmail = requireCompany();
        companyBookmarkMapper.deleteByCompanyEmailAndFreelancerEmail(companyEmail, freelancerEmail);
    }

    private String requireCompany() {
        if (!ROLE_COMPANY.equals(SecurityUtil.getCurrentRole())) {
            throw new CustomException(ErrorCode.COMPANY_ONLY);
        }
        return SecurityUtil.getCurrentEmail();
    }

    private TalentSearchCondition normalizeCondition(TalentSearchCondition condition) {
        TalentSearchCondition normalized = condition == null ? new TalentSearchCondition() : condition;
        normalized.setKeyword(hasText(normalized.getKeyword()) ? normalized.getKeyword().trim() : null);
        normalized.setCategory(normalizeCategory(normalized.getCategory()));
        normalized.setFavoriteOnly(Boolean.TRUE.equals(normalized.getFavoriteOnly()));
        normalized.setSort(normalizeSort(normalized.getSort()));
        normalized.setPage(normalized.getSafePage());
        normalized.setSize(normalized.getSafeSize());
        return normalized;
    }

    private String normalizeCategory(String category) {
        if (!hasText(category)) {
            return null;
        }
        try {
            return JobCategory.valueOf(category.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private String normalizeSort(String sort) {
        if (!hasText(sort)) {
            return DEFAULT_SORT;
        }
        String normalized = sort.trim().toUpperCase(Locale.ROOT);
        if (SORT_VIEW_COUNT.equals(normalized) || SORT_NAME.equals(normalized)) {
            return normalized;
        }
        throw new CustomException(ErrorCode.INVALID_INPUT);
    }

    private void attachTechStacks(TalentListDTO talent) {
        talent.setTechStacks(portfolioProfileMapper.findTechStacks(talent.getFreelancerEmail()));
    }

    private void attachTechStacks(TalentDetailDTO talent) {
        talent.setTechStacks(portfolioProfileMapper.findTechStacks(talent.getFreelancerEmail()));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
