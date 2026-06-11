package com.ssafy.lancit.domain.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioSearchCondition;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.portfolio.service.PortfolioService;
import com.ssafy.lancit.global.enums.FileParentType;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    private static final String USER_EMAIL = "user@test.com";
    private static final String OTHER_EMAIL = "other@test.com";

    @InjectMocks
    private PortfolioService portfolioService;

    @Mock
    private PortfolioMapper portfolioMapper;

    @Mock
    private FileService fileService;

    @Test
    @DisplayName("프로젝트 등록 성공")
    void create_success() {
        PortfolioDTO request = basePortfolio(null, USER_EMAIL);
        request.setBannerFileId(10);
        request.setFileIds(List.of(11, 12));

        given(fileService.findById(10)).willReturn(file(10, USER_EMAIL, FileParentType.TEMP, null));
        given(fileService.findById(11)).willReturn(file(11, USER_EMAIL, FileParentType.TEMP, null));
        given(fileService.findById(12)).willReturn(file(12, USER_EMAIL, FileParentType.PORTFOLIO_FILE, null));
        given(portfolioMapper.insert(any(PortfolioDTO.class))).willAnswer(invocation -> {
            PortfolioDTO dto = invocation.getArgument(0);
            dto.setPortfolioId(1);
            return 1;
        });

        PortfolioDTO created = basePortfolio(1, USER_EMAIL);
        created.setBannerFileId(10);
        given(portfolioMapper.findById(1)).willReturn(created);
        given(fileService.findByParent(FileParentType.PORTFOLIO_FILE, 1))
                .willReturn(List.of(file(11, USER_EMAIL, FileParentType.PORTFOLIO_FILE, 1)));

        PortfolioDTO result = portfolioService.create(request, USER_EMAIL, "USER");

        assertThat(result.getPortfolioId()).isEqualTo(1);
        assertThat(result.isOwner()).isTrue();
        verify(fileService).attachToParent(10, FileParentType.PORTFOLIO_BANNER, 1, USER_EMAIL);
        verify(fileService).attachToParent(11, FileParentType.PORTFOLIO_FILE, 1, USER_EMAIL);
        verify(fileService).attachToParent(12, FileParentType.PORTFOLIO_FILE, 1, USER_EMAIL);
    }

    @Test
    @DisplayName("프로젝트 리스트 조회 성공")
    void getMyList_success() {
        PortfolioDTO card = basePortfolio(1, USER_EMAIL);
        card.setBannerFileId(10);
        given(portfolioMapper.findByEmail(USER_EMAIL, null, null, null, "latest", 0, 10))
                .willReturn(List.of(card));
        given(portfolioMapper.countByEmail(USER_EMAIL, null, null, null)).willReturn(1L);
        given(fileService.findById(10)).willReturn(file(10, USER_EMAIL, FileParentType.PORTFOLIO_BANNER, 1));

        PageResponse<PortfolioDTO> result = portfolioService.getMyList(
                USER_EMAIL,
                "USER",
                new PortfolioSearchCondition()
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBannerFile()).isNotNull();
        assertThat(result.getContent().get(0).isOwner()).isTrue();
    }

    @Test
    @DisplayName("검색/필터 조회 성공")
    void getMyList_searchAndFilter_success() {
        PortfolioSearchCondition condition = new PortfolioSearchCondition();
        condition.setKeyword("랜딩");
        condition.setVisibility("PUBLIC");
        condition.setCategory("web_app");
        condition.setSort("oldest");
        condition.setPage(2);
        condition.setSize(5);

        given(portfolioMapper.findByEmail(USER_EMAIL, "랜딩", true, "WEB_APP", "oldest", 5, 5))
                .willReturn(List.of());
        given(portfolioMapper.countByEmail(USER_EMAIL, "랜딩", true, "WEB_APP")).willReturn(0L);

        PageResponse<PortfolioDTO> result = portfolioService.getMyList(USER_EMAIL, "USER", condition);

        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(5);
        verify(portfolioMapper).findByEmail(USER_EMAIL, "랜딩", true, "WEB_APP", "oldest", 5, 5);
    }

    @Test
    @DisplayName("프로젝트 상세 조회 성공")
    void getOne_success() {
        PortfolioDTO portfolio = basePortfolio(1, USER_EMAIL);
        portfolio.setIsPublic(false);
        portfolio.setBannerFileId(10);
        given(portfolioMapper.findByIdIncludingDeleted(1)).willReturn(portfolio);
        given(fileService.findById(10)).willReturn(file(10, USER_EMAIL, FileParentType.PORTFOLIO_BANNER, 1));
        given(fileService.findByParent(FileParentType.PORTFOLIO_FILE, 1))
                .willReturn(List.of(file(11, USER_EMAIL, FileParentType.PORTFOLIO_FILE, 1)));

        PortfolioDTO result = portfolioService.getOne(1, USER_EMAIL, "USER");

        assertThat(result.isOwner()).isTrue();
        assertThat(result.getFiles()).hasSize(1);
        assertThat(result.getBannerFile().getFileId()).isEqualTo(10);
    }

    @Test
    @DisplayName("본인 프로젝트 수정 성공")
    void update_success() {
        PortfolioDTO existing = basePortfolio(1, USER_EMAIL);
        existing.setBannerFileId(10);

        PortfolioDTO request = PortfolioDTO.builder()
                .title("수정된 프로젝트")
                .bannerFileId(20)
                .fileIds(List.of(21))
                .build();

        PortfolioDTO updated = basePortfolio(1, USER_EMAIL);
        updated.setTitle("수정된 프로젝트");
        updated.setBannerFileId(20);

        given(portfolioMapper.findByIdIncludingDeleted(1)).willReturn(existing, updated);
        given(fileService.findById(20)).willReturn(file(20, USER_EMAIL, FileParentType.TEMP, null));
        given(fileService.findById(21)).willReturn(file(21, USER_EMAIL, FileParentType.TEMP, null));
        given(portfolioMapper.update(1, request)).willReturn(1);
        given(fileService.findByParent(FileParentType.PORTFOLIO_FILE, 1))
                .willReturn(List.of(file(21, USER_EMAIL, FileParentType.PORTFOLIO_FILE, 1)));

        PortfolioDTO result = portfolioService.update(1, request, USER_EMAIL, "USER");

        assertThat(result.getTitle()).isEqualTo("수정된 프로젝트");
        verify(fileService).detach(10);
        verify(fileService).detachByParent(FileParentType.PORTFOLIO_FILE, 1);
        verify(fileService).attachToParent(20, FileParentType.PORTFOLIO_BANNER, 1, USER_EMAIL);
        verify(fileService).attachToParent(21, FileParentType.PORTFOLIO_FILE, 1, USER_EMAIL);
    }

    @Test
    @DisplayName("다른 사람 프로젝트 수정 실패")
    void update_forbidden() {
        PortfolioDTO existing = basePortfolio(1, OTHER_EMAIL);
        PortfolioDTO request = PortfolioDTO.builder().title("수정 시도").build();
        given(portfolioMapper.findByIdIncludingDeleted(1)).willReturn(existing);

        assertThatThrownBy(() -> portfolioService.update(1, request, USER_EMAIL, "USER"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.FORBIDDEN.getMessage());

        verify(portfolioMapper, never()).update(anyInt(), any());
    }

    @Test
    @DisplayName("프로젝트 삭제 성공")
    void delete_success() {
        PortfolioDTO existing = basePortfolio(1, USER_EMAIL);
        given(portfolioMapper.findByIdIncludingDeleted(1)).willReturn(existing);
        given(portfolioMapper.softDelete(1)).willReturn(1);

        portfolioService.delete(1, USER_EMAIL, "USER");

        verify(portfolioMapper, times(1)).softDelete(1);
        verify(fileService, never()).deleteByParent(any(), anyInt());
    }

    @Test
    @DisplayName("삭제된 프로젝트 접근 실패")
    void deletedPortfolio_access_fail() {
        PortfolioDTO deleted = basePortfolio(1, USER_EMAIL);
        deleted.setIsDeleted(true);
        given(portfolioMapper.findByIdIncludingDeleted(1)).willReturn(deleted);

        assertThatThrownBy(() -> portfolioService.getOne(1, USER_EMAIL, "USER"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.DELETED_PORTFOLIO.getMessage());
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦은 경우 실패")
    void create_invalidPeriod_fail() {
        PortfolioDTO request = basePortfolio(null, USER_EMAIL);
        request.setWorkStartAt(LocalDateTime.of(2026, 6, 2, 0, 0));
        request.setWorkEndAt(LocalDateTime.of(2026, 6, 1, 0, 0));

        assertThatThrownBy(() -> portfolioService.create(request, USER_EMAIL, "USER"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_PORTFOLIO_PERIOD.getMessage());
    }

    private PortfolioDTO basePortfolio(Integer portfolioId, String email) {
        return PortfolioDTO.builder()
                .portfolioId(portfolioId == null ? 0 : portfolioId)
                .email(email)
                .category("WEB_APP")
                .title("프로젝트")
                .summary("한줄 소개")
                .content("프로젝트 설명")
                .workStartAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .workEndAt(LocalDateTime.of(2026, 2, 1, 0, 0))
                .isPublic(true)
                .isDeleted(false)
                .createdAt(LocalDateTime.of(2026, 2, 2, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 2, 3, 0, 0))
                .build();
    }

    private FileDTO file(int fileId, String email, FileParentType parentType, Integer parentId) {
        return FileDTO.builder()
                .fileId(fileId)
                .userEmail(email)
                .parentType(parentType)
                .parentId(parentId)
                .oriName("file-" + fileId + ".png")
                .uploadPath("temp/file-" + fileId + ".png")
                .build();
    }
}
