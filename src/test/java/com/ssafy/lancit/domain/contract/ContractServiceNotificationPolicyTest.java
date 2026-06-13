package com.ssafy.lancit.domain.contract;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.chat.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractCancelRequestMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractDocumentMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractFileMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.contract.service.ContractPdfService;
import com.ssafy.lancit.domain.contract.service.ContractService;
import com.ssafy.lancit.domain.contract.validator.ContractValidator;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.NotificationType;

/**
 * ContractService 추가 테스트
 * - rejectContract (신규, Image2 "거절하기")
 * - sendByCompany / sendByFreelancer : 발송자측 PROPOSAL 알림 클리어
 * - cancelContract : 파기 확정 시 양쪽 CONTRACT_CANCEL_REQUEST 알림 클리어
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractServiceNotificationPolicyTest {

    @Mock private ContractMapper contractMapper;
    @Mock private ContractDocumentMapper contractDocumentMapper;
    @Mock private ContractCancelRequestMapper contractCancelRequestMapper;
    @Mock private ContractValidator contractValidator;
    @Mock private NotificationService notificationService;
    @Mock private ContractPdfService contractPdfService;
    @Mock private ContractFileMapper contractFileMapper;
    @Mock private FileService fileService;
    @Mock private ChatRoomMapper chatRoomMapper;

    private ContractService contractService;
    private MockedStatic<SecurityUtil> securityUtilMock;

    private static final String COMPANY_EMAIL = "company@example.com";
    private static final String FREELANCER_EMAIL = "freelancer@example.com";
    private static final Integer CONTRACT_ID = 1;

    @BeforeEach
    void setUp() {
        contractService = new ContractService(
                contractMapper,
                contractDocumentMapper,
                contractCancelRequestMapper,
                contractValidator,
                notificationService,
                new ObjectMapper(),
                contractPdfService,
                contractFileMapper,
                fileService,
                chatRoomMapper
        );

        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    private ContractDTO contractOf(ContractStatus status) {
        return ContractDTO.builder()
                .contractId(CONTRACT_ID)
                .recruitmentId(100)
                .companyEmail(COMPANY_EMAIL)
                .freelancerEmail(FREELANCER_EMAIL)
                .status(status)
                .build();
    }

    // ════════════════════════════════════════════════════
    // rejectContract (WAITING -> 완전삭제, 프리랜서 전용)
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("rejectContract")
    class RejectContract {

        @Test
        @DisplayName("정상 - WAITING 상태에서 프리랜서가 거절하면 deleteContract 호출")
        void success() {
            ContractDTO contract = contractOf(ContractStatus.WAITING);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.rejectContract(CONTRACT_ID);

            verify(contractValidator).validateFreelancer(contract);
            verify(contractValidator).validateWaiting(contract);
            verify(contractMapper).deleteContract(CONTRACT_ID);

            // 별도 cancel-request 절차 없이 즉시 삭제 - cancelRequestMapper는 건드리지 않음
            verify(contractCancelRequestMapper, never()).insert(any());
            verify(contractCancelRequestMapper, never()).existsByContractId(any());
        }

        @Test
        @DisplayName("회사가 거절을 시도하면 FORBIDDEN, deleteContract 호출 안됨")
        void notFreelancer_throws() {
            ContractDTO contract = contractOf(ContractStatus.WAITING);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateFreelancer(contract);

            assertThatThrownBy(() -> contractService.rejectContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(contractMapper, never()).deleteContract(any());
        }

        @Test
        @DisplayName("WAITING 상태가 아니면 INVALID_INPUT (이미 협상 시작된 계약은 거절 불가, cancel-request 절차 이용해야 함)")
        void notWaiting_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.INVALID_INPUT))
                    .when(contractValidator).validateWaiting(contract);

            assertThatThrownBy(() -> contractService.rejectContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(contractMapper, never()).deleteContract(any());
        }

        @Test
        @DisplayName("존재하지 않는 계약이면 NOT_FOUND")
        void contractNotFound_throws() {
            when(contractValidator.getContractOrThrow(CONTRACT_ID))
                    .thenThrow(new CustomException(ErrorCode.NOT_FOUND));

            assertThatThrownBy(() -> contractService.rejectContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(contractMapper, never()).deleteContract(any());
        }

        @Test
        @DisplayName("이미 한 번 거절(삭제)된 contractId로 재호출 시 getContractOrThrow에서 NOT_FOUND")
        void alreadyDeleted_secondCall_notFound() {
            when(contractValidator.getContractOrThrow(CONTRACT_ID))
                    .thenThrow(new CustomException(ErrorCode.NOT_FOUND));

            assertThatThrownBy(() -> contractService.rejectContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }
    }

    // ════════════════════════════════════════════════════
    // sendByCompany - PROPOSAL 알림 클리어
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("sendByCompany - 알림 정책")
    class SendByCompanyNotification {

        @Test
        @DisplayName("발송 시 회사 자신의 PROPOSAL 알림을 클리어하고, 프리랜서에게 새 PROPOSAL 알림을 보낸다")
        void clearsSenderProposal_andNotifiesFreelancer() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.sendByCompany(CONTRACT_ID, new HashMap<>());

            verify(notificationService).markSpecificTypeAsRead(
                    COMPANY_EMAIL, CONTRACT_ID, NotificationType.PROPOSAL);
            verify(notificationService).createNotification(
                    FREELANCER_EMAIL, NotificationType.PROPOSAL, CONTRACT_ID);

            // 프리랜서 측 알림은 클리어하지 않아야 함 (이제 막 알림을 받아야 하는 쪽)
            verify(notificationService, never()).markSpecificTypeAsRead(
                    eq(FREELANCER_EMAIL), eq(CONTRACT_ID), eq(NotificationType.PROPOSAL));
        }

        @Test
        @DisplayName("회사가 아니면 FORBIDDEN - 알림 클리어/생성 모두 호출되지 않는다")
        void notCompany_noNotificationSideEffects() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateCompany(contract);

            assertThatThrownBy(() -> contractService.sendByCompany(CONTRACT_ID, new HashMap<>()))
                    .isInstanceOf(CustomException.class);

            verify(notificationService, never()).markSpecificTypeAsRead(any(), any(), any());
            verify(notificationService, never()).createNotification(any(), any(), any());
        }

        @Test
        @DisplayName("클리어와 신규알림 순서 - 클리어가 먼저 호출된 뒤 신규 알림이 호출된다")
        void clearBeforeCreate_order() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.sendByCompany(CONTRACT_ID, new HashMap<>());

            org.mockito.InOrder inOrder = Mockito.inOrder(notificationService);
            inOrder.verify(notificationService).markSpecificTypeAsRead(
                    COMPANY_EMAIL, CONTRACT_ID, NotificationType.PROPOSAL);
            inOrder.verify(notificationService).createNotification(
                    FREELANCER_EMAIL, NotificationType.PROPOSAL, CONTRACT_ID);
        }
    }

    // ════════════════════════════════════════════════════
    // sendByFreelancer - PROPOSAL 알림 클리어
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("sendByFreelancer - 알림 정책")
    class SendByFreelancerNotification {

        @Test
        @DisplayName("발송 시 프리랜서 자신의 PROPOSAL 알림을 클리어하고, 회사에게 새 PROPOSAL 알림을 보낸다")
        void clearsSenderProposal_andNotifiesCompany() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_B);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.sendByFreelancer(CONTRACT_ID, new HashMap<>());

            verify(notificationService).markSpecificTypeAsRead(
                    FREELANCER_EMAIL, CONTRACT_ID, NotificationType.PROPOSAL);
            verify(notificationService).createNotification(
                    COMPANY_EMAIL, NotificationType.PROPOSAL, CONTRACT_ID);

            verify(notificationService, never()).markSpecificTypeAsRead(
                    eq(COMPANY_EMAIL), eq(CONTRACT_ID), eq(NotificationType.PROPOSAL));
        }

        @Test
        @DisplayName("프리랜서가 아니면 FORBIDDEN - 알림 클리어/생성 모두 호출되지 않는다")
        void notFreelancer_noNotificationSideEffects() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_B);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateFreelancer(contract);

            assertThatThrownBy(() -> contractService.sendByFreelancer(CONTRACT_ID, new HashMap<>()))
                    .isInstanceOf(CustomException.class);

            verify(notificationService, never()).markSpecificTypeAsRead(any(), any(), any());
            verify(notificationService, never()).createNotification(any(), any(), any());
        }
    }

    // ════════════════════════════════════════════════════
    // cancelContract - CONTRACT_CANCEL_REQUEST 알림 양쪽 클리어
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("cancelContract - 알림 정책")
    class CancelContractNotification {

        @Test
        @DisplayName("IN_PROGRESS 파기 확정 시 양쪽(company/freelancer) CONTRACT_CANCEL_REQUEST 알림을 모두 클리어한다")
        void inProgress_clearsBothSidesCancelRequest() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);
            when(contractCancelRequestMapper.findRequesterEmail(CONTRACT_ID)).thenReturn(COMPANY_EMAIL);

            // 요청자(COMPANY)가 아닌 FREELANCER가 승인
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);

            contractService.cancelContract(CONTRACT_ID);

            verify(notificationService).markSpecificTypeAsRead(
                    COMPANY_EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);
            verify(notificationService).markSpecificTypeAsRead(
                    FREELANCER_EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);
        }

        @Test
        @DisplayName("NEGOTIATING 단계 파기(완전삭제)에서도 양쪽 CONTRACT_CANCEL_REQUEST 알림을 클리어한다")
        void negotiating_alsoClearsBothSides() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_B);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);
            when(contractCancelRequestMapper.findRequesterEmail(CONTRACT_ID)).thenReturn(FREELANCER_EMAIL);

            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            contractService.cancelContract(CONTRACT_ID);

            verify(contractMapper).deleteContract(CONTRACT_ID);
            verify(notificationService).markSpecificTypeAsRead(
                    COMPANY_EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);
            verify(notificationService).markSpecificTypeAsRead(
                    FREELANCER_EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);
        }

        @Test
        @DisplayName("파기 요청이 없으면 INVALID_INPUT, 알림 클리어 호출 안됨")
        void noCancelRequest_noNotificationClear() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(false);

            assertThatThrownBy(() -> contractService.cancelContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(notificationService, never()).markSpecificTypeAsRead(any(), any(), any());
        }

        @Test
        @DisplayName("요청자 본인이 파기 시도하면 FORBIDDEN, 알림 클리어 호출 안됨")
        void requesterSelfApprove_noNotificationClear() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);
            when(contractCancelRequestMapper.findRequesterEmail(CONTRACT_ID)).thenReturn(COMPANY_EMAIL);

            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            assertThatThrownBy(() -> contractService.cancelContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(notificationService, never()).markSpecificTypeAsRead(any(), any(), any());
        }

        @Test
        @DisplayName("알림 클리어는 상태변경/삭제가 끝난 뒤에 호출된다 (순서 검증)")
        void clearHappensAfterStatusChange_order() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);
            when(contractCancelRequestMapper.findRequesterEmail(CONTRACT_ID)).thenReturn(COMPANY_EMAIL);

            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);

            contractService.cancelContract(CONTRACT_ID);

            org.mockito.InOrder inOrder = Mockito.inOrder(contractMapper, notificationService);
            inOrder.verify(contractMapper).updateStatus(CONTRACT_ID, ContractStatus.CANCELLED);
            inOrder.verify(notificationService).markSpecificTypeAsRead(
                    eq(COMPANY_EMAIL), eq(CONTRACT_ID), eq(NotificationType.CONTRACT_CANCEL_REQUEST));
        }
    }
}