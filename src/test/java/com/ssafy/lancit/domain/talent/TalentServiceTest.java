package com.ssafy.lancit.domain.talent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.bookmark.company.dto.CompanyBookmarkDTO;
import com.ssafy.lancit.domain.bookmark.company.mapper.CompanyBookmarkMapper;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioProfileMapper;
import com.ssafy.lancit.domain.talent.dto.TalentDetailDTO;
import com.ssafy.lancit.domain.talent.dto.TalentListDTO;
import com.ssafy.lancit.domain.talent.dto.TalentSearchCondition;
import com.ssafy.lancit.domain.talent.mapper.TalentMapper;
import com.ssafy.lancit.domain.talent.service.TalentService;
import com.ssafy.lancit.global.enums.JobCategory;

@ExtendWith(MockitoExtension.class)
class TalentServiceTest {

    private static final String COMPANY_EMAIL = "company@test.com";
    private static final String FREELANCER_EMAIL = "user@test.com";

    @InjectMocks
    private TalentService talentService;

    @Mock
    private TalentMapper talentMapper;

    @Mock
    private PortfolioMapper portfolioMapper;

    @Mock
    private PortfolioProfileMapper portfolioProfileMapper;

    @Mock
    private CompanyBookmarkMapper companyBookmarkMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("회사 계정은 공개 인재 목록을 조회할 수 있다")
    void getTalents_company_success() {
        setAuth(COMPANY_EMAIL, "COMPANY");
        TalentSearchCondition condition = new TalentSearchCondition();
        TalentListDTO talent = talentList();

        given(talentMapper.findTalents(anyString(), any(TalentSearchCondition.class)))
                .willReturn(List.of(talent));
        given(talentMapper.countTalents(anyString(), any(TalentSearchCondition.class))).willReturn(1L);
        given(portfolioProfileMapper.findTechStacks(FREELANCER_EMAIL))
                .willReturn(List.of("JAVA", "SPRING BOOT"));

        PageResponse<TalentListDTO> result = talentService.getTalents(condition);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTechStacks()).containsExactly("JAVA", "SPRING BOOT");
        assertThat(result.getContent().get(0).getShortIntro()).isEqualTo("백엔드 개발자");
        assertThat(TalentListDTO.class.getDeclaredFields())
                .noneMatch(field -> "description".equals(field.getName()));
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("프리랜서 계정은 인재 찾기 목록에 접근할 수 없다")
    void getTalents_freelancer_forbidden() {
        setAuth(FREELANCER_EMAIL, "USER");

        assertThatThrownBy(() -> talentService.getTalents(new TalentSearchCondition()))
                .isInstanceOfSatisfying(CustomException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMPANY_ONLY));
        verify(talentMapper, never()).findTalents(any(), any());
    }

    @Test
    @DisplayName("상세 조회 시 조회수를 증가시키고 공개 프로젝트를 포함한다")
    void getTalentDetail_incrementViewCount_success() {
        setAuth(COMPANY_EMAIL, "COMPANY");
        TalentDetailDTO before = talentDetail(3);
        TalentDetailDTO after = talentDetail(4);
        PortfolioDTO project = PortfolioDTO.builder()
                .portfolioId(1)
                .email(FREELANCER_EMAIL)
                .title("프로젝트")
                .summary("요약")
                .isPublic(true)
                .build();

        given(talentMapper.findTalentDetail(COMPANY_EMAIL, FREELANCER_EMAIL))
                .willReturn(before, after);
        given(portfolioProfileMapper.findTechStacks(FREELANCER_EMAIL)).willReturn(List.of("JAVA"));
        given(portfolioMapper.findPublicProjectsByEmail(FREELANCER_EMAIL)).willReturn(List.of(project));

        TalentDetailDTO result = talentService.getTalentDetail(FREELANCER_EMAIL);

        assertThat(result.getViewCount()).isEqualTo(4);
        assertThat(result.getShortIntro()).isEqualTo("백엔드 개발자");
        assertThat(result.getDescription()).isEqualTo("안정적인 API 설계를 좋아합니다.");
        assertThat(result.getTechStacks()).containsExactly("JAVA");
        assertThat(result.getProjects()).containsExactly(project);
        verify(talentMapper).incrementViewCount(FREELANCER_EMAIL);
    }

    @Test
    @DisplayName("이미 찜한 인재를 다시 찜하면 성공 처리하고 insert하지 않는다")
    void favorite_existing_idempotent() {
        setAuth(COMPANY_EMAIL, "COMPANY");
        given(talentMapper.findTalentDetail(COMPANY_EMAIL, FREELANCER_EMAIL)).willReturn(talentDetail(1));
        given(companyBookmarkMapper.existsByCompanyEmailAndFreelancerEmail(COMPANY_EMAIL, FREELANCER_EMAIL))
                .willReturn(true);

        talentService.favorite(FREELANCER_EMAIL);

        verify(companyBookmarkMapper, never()).insert(any(CompanyBookmarkDTO.class));
    }

    @Test
    @DisplayName("찜하지 않은 인재를 해제해도 성공 처리한다")
    void unfavorite_idempotent() {
        setAuth(COMPANY_EMAIL, "COMPANY");

        talentService.unfavorite(FREELANCER_EMAIL);

        verify(companyBookmarkMapper)
                .deleteByCompanyEmailAndFreelancerEmail(COMPANY_EMAIL, FREELANCER_EMAIL);
    }

    private TalentListDTO talentList() {
        return TalentListDTO.builder()
                .freelancerEmail(FREELANCER_EMAIL)
                .name("홍길동")
                .jobCategory(JobCategory.IT)
                .shortIntro("백엔드 개발자")
                .viewCount(3)
                .isFavorite(false)
                .publicProjectCount(1)
                .build();
    }

    private TalentDetailDTO talentDetail(int viewCount) {
        return TalentDetailDTO.builder()
                .freelancerEmail(FREELANCER_EMAIL)
                .name("홍길동")
                .jobCategory(JobCategory.IT)
                .shortIntro("백엔드 개발자")
                .description("안정적인 API 설계를 좋아합니다.")
                .viewCount(viewCount)
                .isFavorite(false)
                .publicProjectCount(1)
                .build();
    }

    private void setAuth(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
