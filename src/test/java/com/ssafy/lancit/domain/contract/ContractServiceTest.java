package com.ssafy.lancit.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import com.ssafy.lancit.domain.chat.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.chat.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.contract.dto.ContractCancelRequestDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDTO;
import com.ssafy.lancit.domain.contract.dto.ContractDocumentDTO;
import com.ssafy.lancit.domain.contract.dto.ContractFileDTO;
import com.ssafy.lancit.domain.contract.mapper.ContractCancelRequestMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractDocumentMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractFileMapper;
import com.ssafy.lancit.domain.contract.mapper.ContractMapper;
import com.ssafy.lancit.domain.contract.service.ContractPdfService;
import com.ssafy.lancit.domain.contract.service.ContractService;
import com.ssafy.lancit.domain.contract.validator.ContractValidator;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.ContractFileType;
import com.ssafy.lancit.global.enums.ContractStatus;
import com.ssafy.lancit.global.enums.NotificationType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContractServiceTest {

    @Mock private ContractMapper contractMapper;
    @Mock private ContractDocumentMapper contractDocumentMapper;
    @Mock private ContractCancelRequestMapper contractCancelRequestMapper;
    @Mock private ContractValidator contractValidator;
    @Mock private NotificationService notificationService;
    @Mock private ContractPdfService contractPdfService;
    @Mock private ContractFileMapper contractFileMapper;
    @Mock private FileService fileService;
    @Mock private ChatRoomMapper chatRoomMapper;
    @Mock private RecruitmentMapper recruitmentMapper;

    private ContractService contractService;
    private MockedStatic<SecurityUtil> securityUtilMock;

    private static final String COMPANY_EMAIL = "company@lancit.com";
    private static final String FREELANCER_EMAIL = "test@lancit.com";
    private static final Integer CONTRACT_ID = 1;
    private static final Integer RECRUITMENT_ID = 100;

    @BeforeEach
    void setUp() {
        // @RequiredArgsConstructor 순서와 동일하게
        contractService = new ContractService(
                contractMapper,
                contractDocumentMapper,
                contractCancelRequestMapper,
                contractValidator,
                notificationService,
                new ObjectMapper(),
                new ContractPdfService(null), // PDF 생성은 mock으로 대체
                contractFileMapper,
                fileService,
                chatRoomMapper,
                recruitmentMapper
        );
        // ContractPdfService도 mock 필요 - 직접 필드 주입
        injectContractPdfService();
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
    }

    // ContractPdfService는 생성자로 주입되므로, spy 방식으로 교체
    private void injectContractPdfService() {
        try {
            java.lang.reflect.Field f =
                ContractService.class.getDeclaredField("contractPdfService");
            f.setAccessible(true);
            f.set(contractService, contractPdfService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    private ContractDTO contractOf(ContractStatus status) {
        return ContractDTO.builder()
                .contractId(CONTRACT_ID)
                .recruitmentId(RECRUITMENT_ID)
                .companyEmail(COMPANY_EMAIL)
                .freelancerEmail(FREELANCER_EMAIL)
                .status(status)
                .build();
    }

    private RecruitmentDTO recruitmentOf() {
        return RecruitmentDTO.builder()
                .recruitmentId(RECRUITMENT_ID)
                .companyEmail(COMPANY_EMAIL)
                .build();
    }

    // ═══════════════════════════════════════════════════
    // createContract
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("createContract")
    class CreateContract {

        @Test
        @DisplayName("정상 - 계약 생성 + 채팅방 생성 + 프리랜서 PROPOSAL 알림")
        void success() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(recruitmentMapper.findById(RECRUITMENT_ID)).thenReturn(recruitmentOf());
            when(contractMapper.existsActiveContract(RECRUITMENT_ID, FREELANCER_EMAIL)).thenReturn(false);
            when(contractMapper.insert(any())).then(inv -> {
                ContractDTO dto = inv.getArgument(0);
                // insert 후 contractId가 채워진다고 가정 (MyBatis useGeneratedKeys)
                return null;
            });

            Map<String, Object> req = new HashMap<>();
            req.put("recruitmentId", RECRUITMENT_ID);
            req.put("freelancerEmail", FREELANCER_EMAIL);

            contractService.createContract(req);

            verify(contractMapper).insert(any(ContractDTO.class));
            verify(chatRoomMapper).insert(any(ChatRoomDTO.class));
            verify(notificationService).createNotification(
                    eq(FREELANCER_EMAIL), eq(NotificationType.PROPOSAL), any());
        }

        @Test
        @DisplayName("공고가 존재하지 않으면 NOT_FOUND, insert 안됨")
        void recruitmentNotFound_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(recruitmentMapper.findById(RECRUITMENT_ID)).thenReturn(null);

            Map<String, Object> req = new HashMap<>();
            req.put("recruitmentId", RECRUITMENT_ID);
            req.put("freelancerEmail", FREELANCER_EMAIL);

            assertThatThrownBy(() -> contractService.createContract(req))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(contractMapper, never()).insert(any());
            verify(chatRoomMapper, never()).insert(any());
        }

        @Test
        @DisplayName("본인 공고가 아니면 FORBIDDEN")
        void notOwnRecruitment_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn("other@lancit.com");
            RecruitmentDTO rec = RecruitmentDTO.builder()
                    .recruitmentId(RECRUITMENT_ID)
                    .companyEmail(COMPANY_EMAIL) // 다른 회사 공고
                    .build();
            when(recruitmentMapper.findById(RECRUITMENT_ID)).thenReturn(rec);

            Map<String, Object> req = new HashMap<>();
            req.put("recruitmentId", RECRUITMENT_ID);
            req.put("freelancerEmail", FREELANCER_EMAIL);

            assertThatThrownBy(() -> contractService.createContract(req))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(contractMapper, never()).insert(any());
        }

        @Test
        @DisplayName("이미 진행중인 계약이 있으면 INVALID_INPUT, 채팅방 생성/알림 안됨")
        void duplicateActiveContract_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(recruitmentMapper.findById(RECRUITMENT_ID)).thenReturn(recruitmentOf());
            when(contractMapper.existsActiveContract(RECRUITMENT_ID, FREELANCER_EMAIL)).thenReturn(true);

            Map<String, Object> req = new HashMap<>();
            req.put("recruitmentId", RECRUITMENT_ID);
            req.put("freelancerEmail", FREELANCER_EMAIL);

            assertThatThrownBy(() -> contractService.createContract(req))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(contractMapper, never()).insert(any());
            verify(chatRoomMapper, never()).insert(any());
            verify(notificationService, never()).createNotification(any(), any(), any());
        }

        @Test
        @DisplayName("프리랜서에게만 PROPOSAL 알림, 회사 본인에게는 알림 안 감")
        void notification_onlyToFreelancer() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(recruitmentMapper.findById(RECRUITMENT_ID)).thenReturn(recruitmentOf());
            when(contractMapper.existsActiveContract(RECRUITMENT_ID, FREELANCER_EMAIL)).thenReturn(false);

            Map<String, Object> req = new HashMap<>();
            req.put("recruitmentId", RECRUITMENT_ID);
            req.put("freelancerEmail", FREELANCER_EMAIL);

            contractService.createContract(req);

            verify(notificationService, never()).createNotification(
                    eq(COMPANY_EMAIL), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════
    // rejectContract (WAITING -> 완전삭제, 프리랜서 전용)
    // ═══════════════════════════════════════════════════
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
        }

        @Test
        @DisplayName("WAITING이 아니면 validateWaiting에서 INVALID_INPUT, deleteContract 안됨")
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
        @DisplayName("회사가 거절 시도하면 FORBIDDEN")
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
        @DisplayName("존재하지 않는 계약이면 NOT_FOUND")
        void notFound_throws() {
            when(contractValidator.getContractOrThrow(CONTRACT_ID))
                    .thenThrow(new CustomException(ErrorCode.NOT_FOUND));

            assertThatThrownBy(() -> contractService.rejectContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(contractMapper, never()).deleteContract(any());
        }
    }

    // ═══════════════════════════════════════════════════
    // saveDraft (NEGOTIATING_A/B 상태에서 임시저장)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("saveDraft")
    class SaveDraft {

        @Test
        @DisplayName("정상 - NEGOTIATING_A 상태에서 회사가 임시저장하면 contractDocumentMapper.update 호출")
        void success_negotiatingA_company() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            Map<String, Object> req = new HashMap<>();
            req.put("workLocation", "서울");
            req.put("monthlyWage", 3000000);

            contractService.saveDraft(CONTRACT_ID, req);

            verify(contractDocumentMapper).update(any(ContractDocumentDTO.class));
        }

        @Test
        @DisplayName("정상 - NEGOTIATING_B 상태에서 프리랜서가 임시저장 가능")
        void success_negotiatingB_freelancer() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_B);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.saveDraft(CONTRACT_ID, new HashMap<>());

            verify(contractDocumentMapper).update(any(ContractDocumentDTO.class));
        }

        @Test
        @DisplayName("WAITING 상태에서 draft 시도 시 INVALID_INPUT (validateStatus에서)")
        void waitingStatus_throws() {
            ContractDTO contract = contractOf(ContractStatus.WAITING);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.INVALID_INPUT))
                    .when(contractValidator)
                    .validateStatus(contract, ContractStatus.NEGOTIATING_A, ContractStatus.NEGOTIATING_B);

            assertThatThrownBy(() -> contractService.saveDraft(CONTRACT_ID, new HashMap<>()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(contractDocumentMapper, never()).update(any());
        }

        @Test
        @DisplayName("NEGOTIATING_A 상태에서 프리랜서가 임시저장 시도하면 validateCompany에서 FORBIDDEN")
        void negotiatingA_freelancerForbidden() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateCompany(contract);

            assertThatThrownBy(() -> contractService.saveDraft(CONTRACT_ID, new HashMap<>()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(contractDocumentMapper, never()).update(any());
        }

        @Test
        @DisplayName("contractId가 DTO에 set 되는지 확인 (objectMapper 변환 후 setContractId)")
        void contractId_isSetOnDocument() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.saveDraft(CONTRACT_ID, new HashMap<>());

            org.mockito.ArgumentCaptor<ContractDocumentDTO> captor =
                    org.mockito.ArgumentCaptor.forClass(ContractDocumentDTO.class);
            verify(contractDocumentMapper).update(captor.capture());
            assertThat(captor.getValue().getContractId()).isEqualTo(CONTRACT_ID);
        }
    }

    // ═══════════════════════════════════════════════════
    // startContract (WAITING -> NEGOTIATING_A, 회사 전용)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("startContract")
    class StartContract {

        @Test
        @DisplayName("정상 - WAITING에서 회사가 시작하면 document insert + NEGOTIATING_A 상태변경")
        void success() {
            ContractDTO contract = contractOf(ContractStatus.WAITING);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.startContract(CONTRACT_ID);

            verify(contractValidator).validateCompany(contract);
            verify(contractValidator).validateWaiting(contract);
            verify(contractDocumentMapper).insert(any(ContractDocumentDTO.class));
            verify(contractMapper).updateStatus(CONTRACT_ID, ContractStatus.NEGOTIATING_A);
        }

        @Test
        @DisplayName("document insert 후 상태변경 순서 보장 (InOrder)")
        void insertBeforeStatusChange_order() {
            ContractDTO contract = contractOf(ContractStatus.WAITING);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.startContract(CONTRACT_ID);

            InOrder inOrder = Mockito.inOrder(contractDocumentMapper, contractMapper);
            inOrder.verify(contractDocumentMapper).insert(any());
            inOrder.verify(contractMapper).updateStatus(CONTRACT_ID, ContractStatus.NEGOTIATING_A);
        }

        @Test
        @DisplayName("프리랜서가 시작 시도하면 validateCompany에서 FORBIDDEN")
        void notCompany_throws() {
            ContractDTO contract = contractOf(ContractStatus.WAITING);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateCompany(contract);

            assertThatThrownBy(() -> contractService.startContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(contractDocumentMapper, never()).insert(any());
            verify(contractMapper, never()).updateStatus(any(), any());
        }
    }

    // ═══════════════════════════════════════════════════
    // sendByCompany (NEGOTIATING_A -> NEGOTIATING_B)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("sendByCompany - 알림 정책")
    class SendByCompany {

        @Test
        @DisplayName("정상 - 회사 PROPOSAL 클리어 후 프리랜서에게 신규 PROPOSAL 알림 (클리어→생성 순서)")
        void success_clearThenNotify_order() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.sendByCompany(CONTRACT_ID, new HashMap<>());

            InOrder inOrder = Mockito.inOrder(notificationService);
            inOrder.verify(notificationService).markSpecificTypeAsRead(
                    COMPANY_EMAIL, CONTRACT_ID, NotificationType.PROPOSAL);
            inOrder.verify(notificationService).createNotification(
                    FREELANCER_EMAIL, NotificationType.PROPOSAL, CONTRACT_ID);
        }

        @Test
        @DisplayName("상태변경 후 알림 발송 순서 보장")
        void statusChangeBeforeNotify_order() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.sendByCompany(CONTRACT_ID, new HashMap<>());

            InOrder inOrder = Mockito.inOrder(contractMapper, notificationService);
            inOrder.verify(contractMapper).updateStatus(CONTRACT_ID, ContractStatus.NEGOTIATING_B);
            inOrder.verify(notificationService).markSpecificTypeAsRead(any(), any(), any());
        }

        @Test
        @DisplayName("프리랜서는 sendByCompany 불가 (FORBIDDEN)")
        void notCompany_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateCompany(contract);

            assertThatThrownBy(() -> contractService.sendByCompany(CONTRACT_ID, new HashMap<>()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(notificationService, never()).markSpecificTypeAsRead(any(), any(), any());
            verify(notificationService, never()).createNotification(any(), any(), any());
        }

        @Test
        @DisplayName("NEGOTIATING_A가 아니면 validateNegotiatingA에서 INVALID_INPUT")
        void wrongStatus_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_B);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.INVALID_INPUT))
                    .when(contractValidator).validateNegotiatingA(contract);

            assertThatThrownBy(() -> contractService.sendByCompany(CONTRACT_ID, new HashMap<>()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }

    // ═══════════════════════════════════════════════════
    // sendByFreelancer (NEGOTIATING_B -> NEGOTIATING_C)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("sendByFreelancer - 알림 정책")
    class SendByFreelancer {

        @Test
        @DisplayName("정상 - 프리랜서 PROPOSAL 클리어 후 회사에게 신규 PROPOSAL 알림")
        void success_clearThenNotify() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_B);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.sendByFreelancer(CONTRACT_ID, new HashMap<>());

            InOrder inOrder = Mockito.inOrder(notificationService);
            inOrder.verify(notificationService).markSpecificTypeAsRead(
                    FREELANCER_EMAIL, CONTRACT_ID, NotificationType.PROPOSAL);
            inOrder.verify(notificationService).createNotification(
                    COMPANY_EMAIL, NotificationType.PROPOSAL, CONTRACT_ID);
        }

        @Test
        @DisplayName("회사는 sendByFreelancer 불가 (FORBIDDEN)")
        void notFreelancer_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_B);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateFreelancer(contract);

            assertThatThrownBy(() -> contractService.sendByFreelancer(CONTRACT_ID, new HashMap<>()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(notificationService, never()).createNotification(any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════
    // approveContract (NEGOTIATING_C -> IN_PROGRESS + PDF)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("approveContract")
    class ApproveContract {

        @Test
        @DisplayName("정상 - PDF 생성 + GCS 업로드 + contract_file insert + confirmedAt 기록 + IN_PROGRESS")
        void success() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_C);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            ContractDocumentDTO doc = ContractDocumentDTO.builder().contractId(CONTRACT_ID).build();
            when(contractDocumentMapper.findByContractId(CONTRACT_ID)).thenReturn(doc);
            when(contractFileMapper.findPdfByContractId(CONTRACT_ID)).thenReturn(null);
            when(contractPdfService.generateContractPdf(doc)).thenReturn(new byte[]{1, 2, 3});

            FileDTO savedFile = FileDTO.builder().fileId(99).build();
            when(fileService.uploadContractPdf(any(), eq(CONTRACT_ID), eq(COMPANY_EMAIL)))
                    .thenReturn(savedFile);

            contractService.approveContract(CONTRACT_ID);

            verify(contractFileMapper).insert(any(ContractFileDTO.class));
            verify(contractDocumentMapper).updateConfirmedAt(CONTRACT_ID);
            verify(contractMapper).updateStatus(CONTRACT_ID, ContractStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("contract_document가 없으면 NOT_FOUND, PDF 생성 안됨")
        void noDocument_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_C);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractDocumentMapper.findByContractId(CONTRACT_ID)).thenReturn(null);

            assertThatThrownBy(() -> contractService.approveContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(contractPdfService, never()).generateContractPdf(any());
            verify(contractMapper, never()).updateStatus(any(), any());
        }

        @Test
        @DisplayName("이미 PDF가 존재하면 INVALID_INPUT (중복 승인 방지)")
        void alreadyPdfExists_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_C);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractDocumentMapper.findByContractId(CONTRACT_ID))
                    .thenReturn(ContractDocumentDTO.builder().contractId(CONTRACT_ID).build());
            when(contractFileMapper.findPdfByContractId(CONTRACT_ID))
                    .thenReturn(ContractFileDTO.builder().contractFileId(1).type(ContractFileType.PDF).build());

            assertThatThrownBy(() -> contractService.approveContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(contractPdfService, never()).generateContractPdf(any());
        }

        @Test
        @DisplayName("PDF 생성 실패 시 PDF_GENERATION_FAILED, 상태변경/파일 insert 안됨")
        void pdfFails_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_C);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            ContractDocumentDTO doc = ContractDocumentDTO.builder().contractId(CONTRACT_ID).build();
            when(contractDocumentMapper.findByContractId(CONTRACT_ID)).thenReturn(doc);
            when(contractFileMapper.findPdfByContractId(CONTRACT_ID)).thenReturn(null);
            when(contractPdfService.generateContractPdf(doc))
                    .thenThrow(new CustomException(ErrorCode.PDF_GENERATION_FAILED));

            assertThatThrownBy(() -> contractService.approveContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.PDF_GENERATION_FAILED);

            verify(contractMapper, never()).updateStatus(any(), any());
            verify(contractFileMapper, never()).insert(any());
        }

        @Test
        @DisplayName("NEGOTIATING_C가 아니면 validateNegotiatingC에서 INVALID_INPUT")
        void wrongStatus_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.INVALID_INPUT))
                    .when(contractValidator).validateNegotiatingC(contract);

            assertThatThrownBy(() -> contractService.approveContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }

        @Test
        @DisplayName("프리랜서가 승인 시도하면 FORBIDDEN")
        void notCompany_throws() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_C);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateCompany(contract);

            assertThatThrownBy(() -> contractService.approveContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    // ═══════════════════════════════════════════════════
    // completeContract (COMPLETED_PENDING -> COMPLETED, 회사 전용)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("completeContract")
    class CompleteContract {

        @Test
        @DisplayName("정상 - COMPLETED_PENDING에서 회사가 완료 시 COMPLETED + 프리랜서 알림")
        void success() {
            ContractDTO contract = contractOf(ContractStatus.COMPLETED_PENDING);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);

            contractService.completeContract(CONTRACT_ID);

            verify(contractValidator).validateCompany(contract);
            verify(contractValidator).validateCompletedPending(contract);
            verify(contractMapper).updateStatus(CONTRACT_ID, ContractStatus.COMPLETED);
            verify(notificationService).createNotification(
                    FREELANCER_EMAIL, NotificationType.CONTRACT_COMPLETED, CONTRACT_ID);
        }

        @Test
        @DisplayName("회사에게만 알림 권한 (프리랜서 호출 시 FORBIDDEN)")
        void notCompany_throws() {
            ContractDTO contract = contractOf(ContractStatus.COMPLETED_PENDING);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.FORBIDDEN))
                    .when(contractValidator).validateCompany(contract);

            assertThatThrownBy(() -> contractService.completeContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(contractMapper, never()).updateStatus(any(), any());
            verify(notificationService, never()).createNotification(any(), any(), any());
        }

        @Test
        @DisplayName("COMPLETED_PENDING이 아니면 validateCompletedPending에서 INVALID_INPUT")
        void wrongStatus_throws() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.INVALID_INPUT))
                    .when(contractValidator).validateCompletedPending(contract);

            assertThatThrownBy(() -> contractService.completeContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(contractMapper, never()).updateStatus(any(), any());
        }
    }

    // ═══════════════════════════════════════════════════
    // requestCancel (파기 요청)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("requestCancel")
    class RequestCancel {

        @Test
        @DisplayName("정상 - 회사가 파기 요청 시 cancel_request insert + 프리랜서에게 알림")
        void success_companyRequests() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(false);
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            contractService.requestCancel(CONTRACT_ID);

            verify(contractCancelRequestMapper).insert(any(ContractCancelRequestDTO.class));
            verify(notificationService).createNotification(
                    FREELANCER_EMAIL, NotificationType.CONTRACT_CANCEL_REQUEST, CONTRACT_ID);
        }

        @Test
        @DisplayName("정상 - 프리랜서가 파기 요청 시 회사에게 알림")
        void success_freelancerRequests() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_A);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(false);
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);

            contractService.requestCancel(CONTRACT_ID);

            verify(notificationService).createNotification(
                    COMPANY_EMAIL, NotificationType.CONTRACT_CANCEL_REQUEST, CONTRACT_ID);
        }

        @Test
        @DisplayName("이미 파기 요청이 있으면 INVALID_INPUT (중복 방지)")
        void alreadyRequested_throws() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);

            assertThatThrownBy(() -> contractService.requestCancel(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(contractCancelRequestMapper, never()).insert(any());
        }

        @Test
        @DisplayName("COMPLETED/CANCELLED 상태에서 파기 요청 시 validateCancelable에서 INVALID_INPUT")
        void completedStatus_throws() {
            ContractDTO contract = contractOf(ContractStatus.COMPLETED);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            doThrow(new CustomException(ErrorCode.INVALID_INPUT))
                    .when(contractValidator).validateCancelable(contract);

            assertThatThrownBy(() -> contractService.requestCancel(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(contractCancelRequestMapper, never()).insert(any());
        }
    }

    // ═══════════════════════════════════════════════════
    // cancelContract (파기 확정)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("cancelContract")
    class CancelContract {

        @Test
        @DisplayName("IN_PROGRESS -> CANCELLED: PDF 삭제 + 상태변경 + 양쪽 알림 클리어")
        void inProgress_statusChange_and_pdfDelete() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);
            when(contractCancelRequestMapper.findRequesterEmail(CONTRACT_ID)).thenReturn(FREELANCER_EMAIL);
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            ContractFileDTO pdfFile = ContractFileDTO.builder()
                    .contractFileId(1).fileId(99).type(ContractFileType.PDF).build();
            when(contractFileMapper.findPdfByContractId(CONTRACT_ID)).thenReturn(pdfFile);

            contractService.cancelContract(CONTRACT_ID);

            // PDF 파일 삭제
            verify(contractFileMapper).delete(1);
            verify(fileService).deleteBySystem(99);
            // 상태변경 (deleteContract 아님)
            verify(contractMapper).updateStatus(CONTRACT_ID, ContractStatus.CANCELLED);
            verify(contractMapper, never()).deleteContract(any());
            // 양쪽 CANCEL_REQUEST 알림 클리어
            verify(notificationService).markSpecificTypeAsRead(
                    COMPANY_EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);
            verify(notificationService).markSpecificTypeAsRead(
                    FREELANCER_EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);
        }

        @Test
        @DisplayName("IN_PROGRESS에서 PDF가 없으면 deleteContractPdf 내부에서 그냥 return (예외 아님)")
        void inProgress_noPdf_noException() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);
            when(contractCancelRequestMapper.findRequesterEmail(CONTRACT_ID)).thenReturn(FREELANCER_EMAIL);
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(contractFileMapper.findPdfByContractId(CONTRACT_ID)).thenReturn(null);

            contractService.cancelContract(CONTRACT_ID);

            verify(contractMapper).updateStatus(CONTRACT_ID, ContractStatus.CANCELLED);
        }

        @Test
        @DisplayName("NEGOTIATING 단계 완전삭제: contract_file 파일들 deleteBySystem + deleteContract")
        void negotiating_fullDelete() {
            ContractDTO contract = contractOf(ContractStatus.NEGOTIATING_B);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);
            when(contractCancelRequestMapper.findRequesterEmail(CONTRACT_ID)).thenReturn(FREELANCER_EMAIL);
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            List<ContractFileDTO> files = List.of(
                    ContractFileDTO.builder().contractFileId(1).fileId(10).build(),
                    ContractFileDTO.builder().contractFileId(2).fileId(11).build()
            );
            when(contractFileMapper.findByContractId(CONTRACT_ID)).thenReturn(files);

            contractService.cancelContract(CONTRACT_ID);

            verify(fileService).deleteBySystem(10);
            verify(fileService).deleteBySystem(11);
            verify(contractMapper).deleteContract(CONTRACT_ID);
            verify(contractMapper, never()).updateStatus(any(), any());
        }

        @Test
        @DisplayName("파기 요청 없으면 INVALID_INPUT, 삭제/상태변경 안됨")
        void noCancelRequest_throws() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(false);

            assertThatThrownBy(() -> contractService.cancelContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(contractMapper, never()).updateStatus(any(), any());
            verify(contractMapper, never()).deleteContract(any());
        }

        @Test
        @DisplayName("요청자 본인이 파기 확정 시도하면 FORBIDDEN (자기 자신 승인 불가)")
        void selfApprove_throws() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);
            when(contractCancelRequestMapper.findRequesterEmail(CONTRACT_ID)).thenReturn(COMPANY_EMAIL);
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            assertThatThrownBy(() -> contractService.cancelContract(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(contractMapper, never()).updateStatus(any(), any());
            verify(contractMapper, never()).deleteContract(any());
        }

        @Test
        @DisplayName("알림 클리어는 상태변경/삭제 이후에 호출된다 (InOrder)")
        void notifyClearAfterStatusChange_order() {
            ContractDTO contract = contractOf(ContractStatus.IN_PROGRESS);
            when(contractValidator.getContractOrThrow(CONTRACT_ID)).thenReturn(contract);
            when(contractCancelRequestMapper.existsByContractId(CONTRACT_ID)).thenReturn(true);
            when(contractCancelRequestMapper.findRequesterEmail(CONTRACT_ID)).thenReturn(FREELANCER_EMAIL);
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(contractFileMapper.findPdfByContractId(CONTRACT_ID)).thenReturn(null);

            contractService.cancelContract(CONTRACT_ID);

            InOrder inOrder = Mockito.inOrder(contractMapper, notificationService);
            inOrder.verify(contractMapper).updateStatus(CONTRACT_ID, ContractStatus.CANCELLED);
            inOrder.verify(notificationService, times(2)).markSpecificTypeAsRead(any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════
    // getContractDetail
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("getContractDetail")
    class GetContractDetail {

        @Test
        @DisplayName("정상 - document/confirmFiles/pdfFile/cancelRequest 모두 포함 + 알림 읽음처리")
        void success() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);
            Map<String, Object> detail = new HashMap<>();
            detail.put("contractId", CONTRACT_ID);

            when(contractMapper.findContractDetail(CONTRACT_ID)).thenReturn(detail);
            when(contractDocumentMapper.findByContractId(CONTRACT_ID)).thenReturn(null);
            when(contractFileMapper.findConfirmFilesByContractId(CONTRACT_ID)).thenReturn(List.of());
            when(contractFileMapper.findPdfByContractId(CONTRACT_ID)).thenReturn(null);
            when(contractCancelRequestMapper.findByContractId(CONTRACT_ID)).thenReturn(null);

            Map<String, Object> result = contractService.getContractDetail(CONTRACT_ID);

            assertThat(result).containsKeys("document", "confirmFiles", "pdfFile", "cancelRequest");
            verify(notificationService).markContractNotificationsAsRead(FREELANCER_EMAIL, CONTRACT_ID);
        }

        @Test
        @DisplayName("계약이 없으면 NOT_FOUND, 알림 읽음처리 호출 안됨")
        void notFound_throws() {
            when(contractMapper.findContractDetail(CONTRACT_ID)).thenReturn(null);

            assertThatThrownBy(() -> contractService.getContractDetail(CONTRACT_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(notificationService, never()).markContractNotificationsAsRead(any(), any());
        }

        @Test
        @DisplayName("WAITING 상태 계약: document/pdfFile=null, confirmFiles=빈리스트")
        void waitingContract_nullSubresources() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            Map<String, Object> detail = new HashMap<>();
            when(contractMapper.findContractDetail(CONTRACT_ID)).thenReturn(detail);
            when(contractDocumentMapper.findByContractId(CONTRACT_ID)).thenReturn(null);
            when(contractFileMapper.findConfirmFilesByContractId(CONTRACT_ID)).thenReturn(List.of());
            when(contractFileMapper.findPdfByContractId(CONTRACT_ID)).thenReturn(null);
            when(contractCancelRequestMapper.findByContractId(CONTRACT_ID)).thenReturn(null);

            Map<String, Object> result = contractService.getContractDetail(CONTRACT_ID);

            assertThat(result.get("document")).isNull();
            assertThat(result.get("pdfFile")).isNull();
            assertThat((List<?>) result.get("confirmFiles")).isEmpty();
        }
    }
}