package com.ssafy.lancit.domain.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.lancit.common.util.GcsSignedUrlUtil;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.mapper.FileDeleteQueueMapper;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.file.service.GcsService;
import com.ssafy.lancit.global.enums.FileParentType;

@ExtendWith(MockitoExtension.class)
class FileServiceUnitTest {

    @InjectMocks
    private FileService fileService;

    @Mock private FileMapper fileMapper;
    @Mock private GcsService gcsService;
    @Mock private GcsSignedUrlUtil gcsSignedUrlUtil;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private FileDeleteQueueMapper fileDeleteQueueMapper;
    @Mock private MultipartFile multipartFile;

    @Test
    void uploadUserRoleSetsUserEmail() throws Exception {
        given(multipartFile.getOriginalFilename()).willReturn("profile.jpg");
        given(multipartFile.getSize()).willReturn(10L);
        given(gcsService.upload(multipartFile, FileParentType.PROFILE)).willReturn("profile/profile.jpg");

        List<FileDTO> result = fileService.upload(
                List.of(multipartFile),
                FileParentType.PROFILE,
                null,
                "user@test.com",
                "user"
        );

        ArgumentCaptor<FileDTO> captor = ArgumentCaptor.forClass(FileDTO.class);
        verify(fileMapper).insert(captor.capture());
        FileDTO saved = captor.getValue();

        assertThat(result).hasSize(1);
        assertThat(saved.getUserEmail()).isEqualTo("user@test.com");
        assertThat(saved.getCompanyEmail()).isNull();
    }

    @Test
    void uploadCompanyRoleNormalizesUpperCaseAndSetsCompanyEmail() throws Exception {
        given(multipartFile.getOriginalFilename()).willReturn("profile.jpg");
        given(multipartFile.getSize()).willReturn(10L);
        given(gcsService.upload(multipartFile, FileParentType.PROFILE)).willReturn("profile/profile.jpg");

        fileService.upload(
                List.of(multipartFile),
                FileParentType.PROFILE,
                null,
                "company@test.com",
                "COMPANY"
        );

        ArgumentCaptor<FileDTO> captor = ArgumentCaptor.forClass(FileDTO.class);
        verify(fileMapper).insert(captor.capture());
        FileDTO saved = captor.getValue();

        assertThat(saved.getUserEmail()).isNull();
        assertThat(saved.getCompanyEmail()).isEqualTo("company@test.com");
    }
}
