package com.ssafy.lancit.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.dto.ContractFileDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractFileMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.contract.service.ContractFileService;
import com.ssafy.lancit.domain.contract.validator.ContractValidator;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.global.enums.ContractFileType;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.NotificationType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractFileServiceTest {

    @Mock private ContractMapper contractMapper;
    @Mock private ContractFileMapper contractFileMapper;
    @Mock private FileService fileService;
    @Mock private ContractValidator contractValidator;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ContractFileService contractFileService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    private static final Integer CONTRACT_ID = 1;
    private static final Integer FILE_ID = 50;
    private static final Integer CONTRACT_FILE_ID = 10;
    private static final String COMPANY_EMAIL = "company@lancit.com";
    private static final String FREELANCER_EMAIL = "test@lancit.com";

    @BeforeEach
    void setUp() {
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    private ContractDTO contractOf(ContractStatus status) {
        return ContractDTO.builder()
                .contractId(CONTRACT_ID)
                .companyEmail(COMPANY_EMAIL)
                .freelancerEmail(FREELANCER_EMAIL)
                .status(status)
                .build();
    }

    // ═══════════════════════════════════════════════════
    // uploadConfirmFile
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("uploadConfirmFile")
    class UploadConfirmFile {

        @Test
        @DisplayName("정상 - IN_PROGRESS에서 프리랜서가 컨펌파일 업로드 시 CONFIRM 타입으로 insert + 회사에게 알림")
        void success() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractFileService.uploadConfirmFile(CONTRACT_ID, FILE_ID);

            ArgumentCaptor<ContractFileDTO> captor = ArgumentCaptor.forClass(ContractFileDTO.class);
            verify(contractFileMapper).insert(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(ContractFileType.CONFIRM);
            assertThat(captor.getValue().getUploaderEmail()).isEqualTo(FREELANCER_EMAIL);
            assertThat(captor.getValue().getFileId()).isEqualTo(FILE_ID);
            assertThat(captor.getValue().getContractId()).isEqualTo(CONTRACT_ID);

            verify(notificationService).createNotification(
                    COMPANY_EMAIL, NotificationType.CONFIRM_FILE, CONTRACT_ID);
        }

        @Test
        @DisplayName("IN_PROGRESS가 아니면 INVALID_INPUT, insert/알림 안됨")
        void notInProgress_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            assertThatThrownBy(() -> contractFileService.uploadConfirmFile(CONTRACT_ID, FILE_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(contractFileMapper, never()).insert(any());
            verify(notificationService, never()).createNotification(any(), any(), any());
        }

        @Test
        @DisplayName("COMPLETED_PENDING 상태에서도 업로드 불가 (IN_PROGRESS만 허용)")
        void completedPending_throws() {
            ContractDTO contract = contractOf(ContractStatus.COMPLETED_PENDING);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            assertThatThrownBy(() -> contractFileService.uploadConfirmFile(CONTRACT_ID, FILE_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("회사가 업로드 시도하면 validateFreelancer에서 FORBIDDEN")
        void notFreelancer_throws() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateFreelancer(contract);

            assertThatThrownBy(() -> contractFileService.uploadConfirmFile(CONTRACT_ID, FILE_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(contractFileMapper, never()).insert(any());
        }

        @Test
        @DisplayName("존재하지 않는 계약이면 NOT_FOUND")
        void contractNotFound_throws() {
            when(contractValidator.getContractOrThrow(CONTRACT_ID))
                    .thenThrow(new CustomException(ErrorCode.NOT_FOUND));

            assertThatThrownBy(() -> contractFileService.uploadConfirmFile(CONTRACT_ID, FILE_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("uploaderEmail은 현재 로그인 사용자(프리랜서) 이메일로 저장됨")
        void uploaderEmail_isCurrentUser() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractFileService.uploadConfirmFile(CONTRACT_ID, FILE_ID);

            ArgumentCaptor<ContractFileDTO> captor = ArgumentCaptor.forClass(ContractFileDTO.class);
            verify(contractFileMapper).insert(captor.capture());
            assertThat(captor.getValue().getUploaderEmail()).isEqualTo(FREELANCER_EMAIL);
        }

        @Test
        @DisplayName("회사에게만 CONFIRM_FILE 알림, 프리랜서 자신에게는 알림 안 감")
        void notification_onlyToCompany() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractFileService.uploadConfirmFile(CONTRACT_ID, FILE_ID);

            verify(notificationService, never()).createNotification(
                    org.mockito.ArgumentMatchers.eq(FREELANCER_EMAIL), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════
    // getConfirmFiles
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("getConfirmFiles")
    class GetConfirmFiles {

        @Test
        @DisplayName("정상 - 계약 유효성 검사 후 CONFIRM 타입 파일 목록 반환")
        void success() {
            when(contractValidator.getContractOrThrow(CONTRACT_ID))
                    .thenReturn(contractOf(ContractStatus.IN_PROGRESS));

            List<ContractFileDTO> files = List.of(
                    ContractFileDTO.builder().contractFileId(1).fileId(10).type(ContractFileType.CONFIRM).build(),
                    ContractFileDTO.builder().contractFileId(2).fileId(11).type(ContractFileType.CONFIRM).build()
            );
            when(contractFileMapper.findConfirmFilesByContractId(CONTRACT_ID)).thenReturn(files);

            List<ContractFileDTO> result = contractFileService.getConfirmFiles(CONTRACT_ID);

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(f -> f.getType() == ContractFileType.CONFIRM);
        }

        @Test
        @DisplayName("컨펌파일이 없으면 빈 리스트 반환")
        void empty() {
            when(contractValidator.getContractOrThrow(CONTRACT_ID))
                    .thenReturn(contractOf(ContractStatus.IN_PROGRESS));
            when(contractFileMapper.findConfirmFilesByContractId(CONTRACT_ID))
                    .thenReturn(List.of());

            List<ContractFileDTO> result = contractFileService.getConfirmFiles(CONTRACT_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 계약이면 NOT_FOUND")
        void contractNotFound_throws() {
            when(contractValidator.getContractOrThrow(CONTRACT_ID))
                    .thenThrow(new CustomException(ErrorCode.NOT_FOUND));

            assertThatThrownBy(() -> contractFileService.getConfirmFiles(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(contractFileMapper, never()).findConfirmFilesByContractId(any());
        }
    }

    // ═══════════════════════════════════════════════════
    // deleteConfirmFile
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("deleteConfirmFile")
    class DeleteConfirmFile {

        @Test
        @DisplayName("정상 - IN_PROGRESS에서 프리랜서가 삭제 시 contract_file 삭제 + file 삭제")
        void success() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            ContractFileDTO file = ContractFileDTO.builder()
                    .contractFileId(CONTRACT_FILE_ID).fileId(FILE_ID)
                    .type(ContractFileType.CONFIRM).build();
            when(contractFileMapper.findById(CONTRACT_FILE_ID)).thenReturn(file);

            contractFileService.deleteConfirmFile(CONTRACT_ID, CONTRACT_FILE_ID);

            verify(contractFileMapper).delete(CONTRACT_FILE_ID);
            verify(fileService).delete(FILE_ID);
        }

        @Test
        @DisplayName("IN_PROGRESS가 아니면 INVALID_INPUT, 삭제 안됨")
        void notInProgress_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            assertThatThrownBy(() -> contractFileService.deleteConfirmFile(CONTRACT_ID, CONTRACT_FILE_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("컨펌파일이 없으면 NOT_FOUND")
        void fileNotFound_throws() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractFileMapper.findById(CONTRACT_FILE_ID)).thenReturn(null);

            assertThatThrownBy(() -> contractFileService.deleteConfirmFile(CONTRACT_ID, CONTRACT_FILE_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("회사가 삭제 시도하면 validateFreelancer에서 FORBIDDEN")
        void notFreelancer_throws() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateFreelancer(contract);

            assertThatThrownBy(() -> contractFileService.deleteConfirmFile(CONTRACT_ID, CONTRACT_FILE_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(contractFileMapper, never()).delete(anyInt());
        }

        @Test
        @DisplayName("contract_file 삭제 순서: contractFileMapper.delete 후 fileService.delete")
        void deleteOrder_contractFileThenFile() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractFileMapper.findById(CONTRACT_FILE_ID)).thenReturn(
                    ContractFileDTO.builder().contractFileId(CONTRACT_FILE_ID)
                            .fileId(FILE_ID).type(ContractFileType.CONFIRM).build());

            contractFileService.deleteConfirmFile(CONTRACT_ID, CONTRACT_FILE_ID);

            org.mockito.InOrder inOrder = Mockito.inOrder(contractFileMapper, fileService);
            inOrder.verify(contractFileMapper).delete(CONTRACT_FILE_ID);
            inOrder.verify(fileService).delete(FILE_ID);
        }

        @Test
        @DisplayName("COMPLETED 상태에서 삭제 시도 시 INVALID_INPUT")
        void completedStatus_throws() {
            ContractDTO contract = contractOf(ContractStatus.COMPLETED);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            assertThatThrownBy(() -> contractFileService.deleteConfirmFile(CONTRACT_ID, CONTRACT_FILE_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }
}