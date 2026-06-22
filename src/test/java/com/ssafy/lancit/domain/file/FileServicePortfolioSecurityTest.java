package com.ssafy.lancit.domain.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.GcsSignedUrlUtil;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.mapper.FileDeleteQueueMapper;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.file.service.GcsService;
import com.ssafy.lancit.domain.portfolio.dto.PortfolioDTO;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationPortfolioSnapshotMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationProfileSnapshotMapper;
import com.ssafy.lancit.global.enums.FileParentType;

@ExtendWith(MockitoExtension.class)
class FileServicePortfolioSecurityTest {

    private static final String USER_EMAIL = "owner@test.com";

    @InjectMocks private FileService fileService;
    @Mock private FileMapper fileMapper;
    @Mock private GcsService gcsService;
    @Mock private GcsSignedUrlUtil gcsSignedUrlUtil;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FileDeleteQueueMapper fileDeleteQueueMapper;
    @Mock private PortfolioMapper portfolioMapper;
    @Mock private ApplicationPortfolioSnapshotMapper applicationPortfolioSnapshotMapper;
    @Mock private ApplicationProfileSnapshotMapper applicationProfileSnapshotMapper;
    @Mock private CacheManager cacheManager;

    @Test
    void uploadPortfolioFile_toOtherUsersPortfolio_isRejectedBeforeGcsUpload() throws Exception {
        given(portfolioMapper.findById(1)).willReturn(
                PortfolioDTO.builder().portfolioId(1).email("other@test.com").build());
        MockMultipartFile file = new MockMultipartFile("files", "result.pdf", "application/pdf", new byte[] {1});

        assertError(
                () -> fileService.upload(List.of(file), FileParentType.PORTFOLIO_FILE, 1, USER_EMAIL, "USER"),
                ErrorCode.FORBIDDEN);

        verify(gcsService, never()).upload(any(), any());
        verify(fileMapper, never()).insert(any());
    }

    @Test
    void uploadPortfolioBanner_companyRole_isRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "banner.png", "image/png", new byte[] {1});

        assertError(
                () -> fileService.upload(List.of(file), FileParentType.PORTFOLIO_BANNER, 1,
                        "company@test.com", "COMPANY"),
                ErrorCode.FORBIDDEN);

        verify(portfolioMapper, never()).findById(1);
        verify(gcsService, never()).upload(any(), any());
    }

    @Test
    void uploadPortfolioFile_withoutParentId_isRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "result.pdf", "application/pdf", new byte[] {1});

        assertError(
                () -> fileService.upload(List.of(file), FileParentType.PORTFOLIO_FILE, null, USER_EMAIL, "USER"),
                ErrorCode.INVALID_INPUT);

        verify(gcsService, never()).upload(any(), any());
    }

    @Test
    void attachTempBanner_promotesAndConnectsToPortfolio() {
        FileDTO temp = FileDTO.builder()
                .fileId(10)
                .userEmail(USER_EMAIL)
                .parentType(FileParentType.TEMP)
                .sysName("temp/banner.png")
                .build();
        given(fileMapper.findById(10)).willReturn(temp);
        given(gcsService.move("temp/banner.png", FileParentType.PORTFOLIO_BANNER))
                .willReturn("portfolio/banner/banner.png");

        fileService.attachToParent(10, FileParentType.PORTFOLIO_BANNER, 1, USER_EMAIL);

        verify(fileMapper).updatePath(10, "portfolio/banner/banner.png");
        verify(fileMapper).updateParent(10, FileParentType.PORTFOLIO_BANNER, 1);
    }

    @Test
    void attachOtherUsersTempBanner_isRejected() {
        FileDTO temp = FileDTO.builder()
                .fileId(10)
                .userEmail("other@test.com")
                .parentType(FileParentType.TEMP)
                .sysName("temp/banner.png")
                .build();
        given(fileMapper.findById(10)).willReturn(temp);

        assertError(
                () -> fileService.attachToParent(10, FileParentType.PORTFOLIO_BANNER, 1, USER_EMAIL),
                ErrorCode.FORBIDDEN);

        verify(gcsService, never()).move(any(), any());
        verify(fileMapper, never()).updateParent(anyInt(), any(), any());
    }

    @Test
    void attachTempBanner_transactionRollback_restoresTempPath() {
        FileDTO temp = FileDTO.builder()
                .fileId(10)
                .userEmail(USER_EMAIL)
                .parentType(FileParentType.TEMP)
                .sysName("temp/banner.png")
                .build();
        given(fileMapper.findById(10)).willReturn(temp);
        given(gcsService.move("temp/banner.png", FileParentType.PORTFOLIO_BANNER))
                .willReturn("portfolio/banner/banner.png");

        TransactionSynchronizationManager.initSynchronization();
        try {
            fileService.attachToParent(10, FileParentType.PORTFOLIO_BANNER, 1, USER_EMAIL);
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(gcsService).move("portfolio/banner/banner.png", FileParentType.TEMP);
    }

    private void assertError(ThrowingCallable callable, ErrorCode expected) {
        assertThatThrownBy(callable::call)
                .isInstanceOfSatisfying(CustomException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(expected));
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call() throws Exception;
    }
}
