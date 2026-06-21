package com.ssafy.lancit.domain.file;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;

import com.ssafy.lancit.common.util.GcsSignedUrlUtil;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.event.FileDeleteEvent;
import com.ssafy.lancit.domain.file.mapper.FileDeleteQueueMapper;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.file.service.GcsService;
import com.ssafy.lancit.domain.portfolio.mapper.PortfolioMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationPortfolioSnapshotMapper;
import com.ssafy.lancit.domain.recruitment.application.mapper.ApplicationProfileSnapshotMapper;

@ExtendWith(MockitoExtension.class)
class FileServiceSnapshotCleanupTest {

    @InjectMocks
    private FileService fileService;

    @Mock private FileMapper fileMapper;
    @Mock private GcsService gcsService;
    @Mock private GcsSignedUrlUtil gcsSignedUrlUtil;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FileDeleteQueueMapper fileDeleteQueueMapper;
    @Mock private PortfolioMapper portfolioMapper;
    @Mock private ApplicationPortfolioSnapshotMapper applicationPortfolioSnapshotMapper;
    @Mock private ApplicationProfileSnapshotMapper applicationProfileSnapshotMapper;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @Test
    void snapshotReferencedFile_isDetachedInsteadOfDeleted() {
        FileDTO file = FileDTO.builder().fileId(10).uploadPath("portfolio/file/result.pdf").build();
        given(fileMapper.findById(10)).willReturn(file);
        given(applicationPortfolioSnapshotMapper.isFileReferenced(10)).willReturn(true);

        fileService.deleteBySystem(10);

        verify(fileMapper).detach(10);
        verify(fileMapper, never()).delete(10);
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(FileDeleteEvent.class));
    }

    @Test
    void unreferencedFile_isDeletedAfterCommitEventPublished() {
        FileDTO file = FileDTO.builder().fileId(10).uploadPath("portfolio/file/result.pdf").build();
        given(fileMapper.findById(10)).willReturn(file);
        given(cacheManager.getCache("signedUrl")).willReturn(cache);

        fileService.deleteBySystem(10);

        verify(fileMapper).delete(10);
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(FileDeleteEvent.class));
        verify(cache).evict(10);
    }

    @Test
    void currentProfileReference_preventsOldProfileDeletion() {
        given(fileMapper.isCurrentProfileFileReferenced(10)).willReturn(true);

        fileService.deleteProfileIfUnreferenced(10);

        verify(fileMapper, never()).findById(10);
        verify(fileMapper, never()).delete(10);
    }

    @Test
    void detachedFileWithoutSnapshotReference_isDeleted() {
        FileDTO file = FileDTO.builder().fileId(10).uploadPath("portfolio/file/old.pdf").build();
        given(fileMapper.findById(10)).willReturn(file);
        given(cacheManager.getCache("signedUrl")).willReturn(cache);

        fileService.deletePortfolioFileIfUnreferenced(10);

        verify(fileMapper).delete(10);
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(FileDeleteEvent.class));
    }

    @Test
    void currentRecruitmentReference_preventsImageDeletion() {
        given(fileMapper.isCurrentRecruitmentImageReferenced(10)).willReturn(true);

        fileService.deleteRecruitmentImageIfUnreferenced(10);

        verify(fileMapper, never()).findById(10);
        verify(fileMapper, never()).delete(10);
    }

    @Test
    void unreferencedRecruitmentImage_isDeletedAfterCommitEventPublished() {
        FileDTO file = FileDTO.builder().fileId(10).uploadPath("recruitment/image/old.png").build();
        given(fileMapper.isCurrentRecruitmentImageReferenced(10)).willReturn(false);
        given(fileMapper.findById(10)).willReturn(file);
        given(cacheManager.getCache("signedUrl")).willReturn(cache);

        fileService.deleteRecruitmentImageIfUnreferenced(10);

        verify(fileMapper).delete(10);
        verify(applicationProfileSnapshotMapper, never()).isProfileFileReferenced(10);
        verify(applicationPortfolioSnapshotMapper, never()).isFileReferenced(10);
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(FileDeleteEvent.class));
        verify(cache).evict(10);
    }
}
