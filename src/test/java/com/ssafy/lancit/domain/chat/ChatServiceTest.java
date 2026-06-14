package com.ssafy.lancit.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.chat.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.chat.dto.MessageDTO;
import com.ssafy.lancit.domain.chat.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.chat.mapper.MessageFileMapper;
import com.ssafy.lancit.domain.chat.mapper.MessageMapper;
import com.ssafy.lancit.domain.chat.service.ChatService;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.global.enums.MessageType;
import com.ssafy.lancit.global.enums.NotificationType;

/**
 * ChatService 단위 테스트
 *
 * 실제 ChatService 필드 순서:
 * ChatRoomMapper, MessageMapper, MessageFileMapper,
 * NotificationService, SimpMessagingTemplate, FileService
 *
 * sendMessage/sendFileMessage는 senderEmail을 파라미터로 받음
 * (STOMP Principal에서 꺼낸 값을 ChatStompController가 넘김 - SecurityUtil 미사용)
 *
 * deleteMessage/updateMessage는 REST 엔드포인트라 SecurityUtil.getCurrentEmail() 사용
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock private ChatRoomMapper chatRoomMapper;
    @Mock private MessageMapper messageMapper;
    @Mock private MessageFileMapper messageFileMapper;
    @Mock private NotificationService notificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private FileService fileService;

    @InjectMocks
    private ChatService chatService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    private static final Integer CHAT_ROOM_ID = 5;
    private static final Integer CONTRACT_ID = 9;
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

    private ChatRoomDTO roomOf() {
        return ChatRoomDTO.builder()
                .chatRoomId(CHAT_ROOM_ID)
                .contractId(CONTRACT_ID)
                .companyEmail(COMPANY_EMAIL)
                .freelancerEmail(FREELANCER_EMAIL)
                .build();
    }

    private MessageDTO messageOf(Integer id, String sender, boolean deleted, boolean updated) {
        return MessageDTO.builder()
                .messageId(id)
                .chatRoomId(CHAT_ROOM_ID)
                .senderEmail(sender)
                .messageType(MessageType.TEXT)
                .message("테스트메시지")
                .isDeleted(deleted)
                .isUpdated(updated)
                .build();
    }

    // ═══════════════════════════════════════════════════
    // sendMessage
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("정상 - 회사가 보내면 프리랜서에게 CHAT 알림 발송")
        void success_companyToFreelancer() {
            MessageDTO saved = messageOf(1, COMPANY_EMAIL, false, false);
            when(messageMapper.findById(any())).thenReturn(saved);
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            MessageDTO result = chatService.sendMessage(
                    CHAT_ROOM_ID, "안녕하세요", COMPANY_EMAIL, MessageType.TEXT);

            // insert 파라미터 확인
            ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
            verify(messageMapper).insert(captor.capture());
            assertThat(captor.getValue().getSenderEmail()).isEqualTo(COMPANY_EMAIL);
            assertThat(captor.getValue().getMessage()).isEqualTo("안녕하세요");
            assertThat(captor.getValue().getChatRoomId()).isEqualTo(CHAT_ROOM_ID);

            // 알림은 프리랜서(수신자)에게
            verify(notificationService).createNotification(
                    FREELANCER_EMAIL, NotificationType.CHAT, CONTRACT_ID);

            assertThat(result).isEqualTo(saved);
        }

        @Test
        @DisplayName("정상 - 프리랜서가 보내면 회사에게 CHAT 알림 발송")
        void success_freelancerToCompany() {
            when(messageMapper.findById(any()))
                    .thenReturn(messageOf(2, FREELANCER_EMAIL, false, false));
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendMessage(CHAT_ROOM_ID, "네 안녕하세요", FREELANCER_EMAIL, MessageType.TEXT);

            verify(notificationService).createNotification(
                    COMPANY_EMAIL, NotificationType.CHAT, CONTRACT_ID);
        }

        @Test
        @DisplayName("발신자 본인에게는 알림이 가지 않는다")
        void noSelfNotification() {
            when(messageMapper.findById(any()))
                    .thenReturn(messageOf(1, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendMessage(CHAT_ROOM_ID, "hi", COMPANY_EMAIL, MessageType.TEXT);

            verify(notificationService, never()).createNotification(
                    eq(COMPANY_EMAIL), any(), any());
        }

        @Test
        @DisplayName("빈 문자열 메시지도 insert됨 (서비스 단 내용 검증 없음)")
        void emptyContent_inserted() {
            when(messageMapper.findById(any()))
                    .thenReturn(messageOf(3, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendMessage(CHAT_ROOM_ID, "", COMPANY_EMAIL, MessageType.TEXT);

            ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
            verify(messageMapper).insert(captor.capture());
            assertThat(captor.getValue().getMessage()).isEmpty();
        }

        @Test
        @DisplayName("chatRoomMapper.findById가 null이면 createChatNotification 내부에서 NPE 발생 (방어 로직 없음)")
        void roomNotFound_npe() {
            when(messageMapper.findById(any()))
                    .thenReturn(messageOf(4, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(null);

            assertThatThrownBy(() ->
                    chatService.sendMessage(CHAT_ROOM_ID, "hi", COMPANY_EMAIL, MessageType.TEXT))
                    .isInstanceOf(NullPointerException.class);

            verify(messageMapper).insert(any());
        }

        @Test
        @DisplayName("MessageType.FILE 타입으로도 sendMessage 호출 가능")
        void fileType_works() {
            when(messageMapper.findById(any()))
                    .thenReturn(messageOf(5, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendMessage(CHAT_ROOM_ID, "", COMPANY_EMAIL, MessageType.FILE);

            ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
            verify(messageMapper).insert(captor.capture());
            assertThat(captor.getValue().getMessageType()).isEqualTo(MessageType.FILE);
        }
    }

    // ═══════════════════════════════════════════════════
    // sendFileMessage
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("sendFileMessage")
    class SendFileMessage {

        @Test
        @DisplayName("정상 - message insert + messageFile insert + 알림 발송")
        void success() {
            MessageDTO saved = MessageDTO.builder()
                    .messageId(10).chatRoomId(CHAT_ROOM_ID)
                    .senderEmail(COMPANY_EMAIL).messageType(MessageType.FILE)
                    .build();
            when(messageMapper.findById(any())).thenReturn(saved);
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendFileMessage(CHAT_ROOM_ID, 999, COMPANY_EMAIL, MessageType.FILE);

            verify(messageMapper).insert(any(MessageDTO.class));
            verify(messageFileMapper).insert(any(), eq(999));
            verify(notificationService).createNotification(
                    FREELANCER_EMAIL, NotificationType.CHAT, CONTRACT_ID);
        }

        @Test
        @DisplayName("message 필드는 빈 문자열로 insert됨 (NULL 방지)")
        void message_isEmptyString() {
            when(messageMapper.findById(any())).thenReturn(
                    MessageDTO.builder().messageId(11).chatRoomId(CHAT_ROOM_ID)
                            .senderEmail(COMPANY_EMAIL).messageType(MessageType.FILE).build());
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendFileMessage(CHAT_ROOM_ID, 999, COMPANY_EMAIL, MessageType.FILE);

            ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
            verify(messageMapper).insert(captor.capture());
            assertThat(captor.getValue().getMessage()).isEqualTo("");
        }

        @Test
        @DisplayName("fileId가 null이어도 messageFileMapper.insert 호출됨 (서비스 단 null 검증 없음)")
        void nullFileId_passthrough() {
            when(messageMapper.findById(any())).thenReturn(
                    MessageDTO.builder().messageId(12).chatRoomId(CHAT_ROOM_ID)
                            .senderEmail(FREELANCER_EMAIL).messageType(MessageType.FILE).build());
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendFileMessage(CHAT_ROOM_ID, null, FREELANCER_EMAIL, MessageType.FILE);

            verify(messageFileMapper).insert(any(), eq((Integer) null));
        }
    }

    // ═══════════════════════════════════════════════════
    // getMessagesByContractId
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("getMessagesByContractId")
    class GetMessagesByContractId {

        @Test
        @DisplayName("정상 - size 없으면 기본 30개로 조회")
        void success_defaultSize() {
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());
            when(messageMapper.findMessages(CHAT_ROOM_ID, null, 30))
                    .thenReturn(List.of(messageOf(1, COMPANY_EMAIL, false, false)));

            List<MessageDTO> result = chatService.getMessagesByContractId(CONTRACT_ID, null, null);

            assertThat(result).hasSize(1);
            verify(messageMapper).findMessages(CHAT_ROOM_ID, null, 30);
        }

        @Test
        @DisplayName("size 파라미터 지정 시 해당 값으로 조회")
        void success_customSize() {
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());
            when(messageMapper.findMessages(CHAT_ROOM_ID, 50, 10)).thenReturn(List.of());

            chatService.getMessagesByContractId(CONTRACT_ID, 50, 10);

            verify(messageMapper).findMessages(CHAT_ROOM_ID, 50, 10);
        }

        @Test
        @DisplayName("채팅방이 없으면 NOT_FOUND, messageMapper 호출 안됨")
        void roomNotFound_throws() {
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(null);

            assertThatThrownBy(() -> chatService.getMessagesByContractId(CONTRACT_ID, null, null))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(messageMapper, never()).findMessages(any(), any(), any());
        }

        @Test
        @DisplayName("메시지가 없으면 빈 리스트 반환")
        void empty_messages() {
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());
            when(messageMapper.findMessages(CHAT_ROOM_ID, null, 30)).thenReturn(Collections.emptyList());

            List<MessageDTO> result = chatService.getMessagesByContractId(CONTRACT_ID, null, null);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════
    // deleteMessage
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("deleteMessage")
    class DeleteMessage {

        @Test
        @DisplayName("정상 - 파일삭제 + messageFile 삭제 + softDelete + STOMP 브로드캐스트")
        void success_fullFlow() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO original = messageOf(100, COMPANY_EMAIL, false, false);
            MessageDTO afterDelete = messageOf(100, COMPANY_EMAIL, true, false);
            when(messageMapper.findById(100)).thenReturn(original, afterDelete);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());
            when(messageFileMapper.findFileIdsByMessageId(100)).thenReturn(List.of(55, 66));

            chatService.deleteMessage(CONTRACT_ID, 100);

            // 첨부파일 삭제
            verify(fileService).deleteBySystem(55);
            verify(fileService).deleteBySystem(66);
            verify(messageFileMapper).deleteByMessageId(100);
            // 소프트 삭제
            verify(messageMapper).softDelete(100);
            // 브로드캐스트
            ArgumentCaptor<MessageDTO> broadcastCaptor = ArgumentCaptor.forClass(MessageDTO.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/chat/" + CHAT_ROOM_ID), (Object) broadcastCaptor.capture());
            assertThat(broadcastCaptor.getValue().isDeleted()).isTrue();
        }

        @Test
        @DisplayName("첨부파일 없는 메시지 삭제 시 fileService.deleteBySystem 호출 안됨")
        void noFiles_softDeleteOnly() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO msg = messageOf(101, COMPANY_EMAIL, false, false);
            when(messageMapper.findById(101)).thenReturn(msg, messageOf(101, COMPANY_EMAIL, true, false));
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());
            when(messageFileMapper.findFileIdsByMessageId(101)).thenReturn(Collections.emptyList());

            chatService.deleteMessage(CONTRACT_ID, 101);

            verify(fileService, never()).deleteBySystem(any(Integer.class));
            verify(messageMapper).softDelete(101);
        }

        @Test
        @DisplayName("존재하지 않는 메시지 삭제 시 NOT_FOUND")
        void messageNotFound_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(999)).thenReturn(null);

            assertThatThrownBy(() -> chatService.deleteMessage(CONTRACT_ID, 999))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(messageMapper, never()).softDelete(any());
            verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
        }

        @Test
        @DisplayName("타인 메시지 삭제 시 FORBIDDEN")
        void notOwner_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);
            when(messageMapper.findById(100)).thenReturn(messageOf(100, COMPANY_EMAIL, false, false));

            assertThatThrownBy(() -> chatService.deleteMessage(CONTRACT_ID, 100))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(messageMapper, never()).softDelete(any());
        }

        @Test
        @DisplayName("contractId에 해당하는 채팅방이 없으면 INVALID_INPUT")
        void roomNotFound_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(100)).thenReturn(messageOf(100, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(null);

            assertThatThrownBy(() -> chatService.deleteMessage(CONTRACT_ID, 100))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(messageMapper, never()).softDelete(any());
        }

        @Test
        @DisplayName("메시지의 chatRoomId가 contractId에서 조회한 chatRoomId와 다르면 INVALID_INPUT")
        void chatRoomMismatch_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO msgOtherRoom = MessageDTO.builder()
                    .messageId(100).chatRoomId(999) // 다른 채팅방
                    .senderEmail(COMPANY_EMAIL).build();
            when(messageMapper.findById(100)).thenReturn(msgOtherRoom);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf()); // chatRoomId=5

            assertThatThrownBy(() -> chatService.deleteMessage(CONTRACT_ID, 100))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(messageMapper, never()).softDelete(any());
        }

        @Test
        @DisplayName("파일 삭제 중 예외 발생 시 softDelete/브로드캐스트 호출 안됨 (트랜잭션 롤백)")
        void fileDeleteFails_softDeleteNotCalled() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            when(messageMapper.findById(100)).thenReturn(messageOf(100, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());
            when(messageFileMapper.findFileIdsByMessageId(100)).thenReturn(List.of(55));
            doThrow(new RuntimeException("GCS error")).when(fileService).deleteBySystem(55);

            assertThatThrownBy(() -> chatService.deleteMessage(CONTRACT_ID, 100))
                    .isInstanceOf(RuntimeException.class);

            verify(messageMapper, never()).softDelete(any());
            verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
        }
    }

    // ═══════════════════════════════════════════════════
    // updateMessage
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("updateMessage")
    class UpdateMessage {

        @Test
        @DisplayName("정상 - 본인 메시지 수정 + STOMP 브로드캐스트")
        void success() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);

            MessageDTO original = messageOf(200, FREELANCER_EMAIL, false, false);
            MessageDTO afterUpdate = MessageDTO.builder()
                    .messageId(200).chatRoomId(CHAT_ROOM_ID).senderEmail(FREELANCER_EMAIL)
                    .messageType(MessageType.TEXT).message("수정됨").isUpdated(true).build();

            when(messageMapper.findById(200)).thenReturn(original, afterUpdate);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());

            chatService.updateMessage(CONTRACT_ID, 200, "수정됨");

            verify(messageMapper).update(200, "수정됨");

            ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/chat/" + CHAT_ROOM_ID), (Object) captor.capture());
            assertThat(captor.getValue().isUpdated()).isTrue();
            assertThat(captor.getValue().getMessage()).isEqualTo("수정됨");
        }

        @Test
        @DisplayName("존재하지 않는 메시지 수정 시 NOT_FOUND")
        void messageNotFound_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(999)).thenReturn(null);

            assertThatThrownBy(() -> chatService.updateMessage(CONTRACT_ID, 999, "수정"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(messageMapper, never()).update(any(), any());
            verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
        }

        @Test
        @DisplayName("타인 메시지 수정 시 FORBIDDEN")
        void notOwner_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);
            when(messageMapper.findById(200)).thenReturn(messageOf(200, COMPANY_EMAIL, false, false));

            assertThatThrownBy(() -> chatService.updateMessage(CONTRACT_ID, 200, "수정"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(messageMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("채팅방 없으면 INVALID_INPUT")
        void roomNotFound_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(200)).thenReturn(messageOf(200, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(null);

            assertThatThrownBy(() -> chatService.updateMessage(CONTRACT_ID, 200, "수정"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(messageMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("메시지 chatRoomId와 contractId 기반 chatRoomId 불일치 시 INVALID_INPUT")
        void chatRoomMismatch_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO msgOtherRoom = MessageDTO.builder()
                    .messageId(200).chatRoomId(888)
                    .senderEmail(COMPANY_EMAIL).build();
            when(messageMapper.findById(200)).thenReturn(msgOtherRoom);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());

            assertThatThrownBy(() -> chatService.updateMessage(CONTRACT_ID, 200, "수정"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(messageMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("content가 null이어도 mapper.update(messageId, null) 그대로 호출")
        void nullContent_passthrough() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO msg = messageOf(201, COMPANY_EMAIL, false, false);
            when(messageMapper.findById(201)).thenReturn(msg, msg);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());

            chatService.updateMessage(CONTRACT_ID, 201, null);

            verify(messageMapper).update(201, null);
        }

        @Test
        @DisplayName("이미 삭제된 메시지(isDeleted=true)도 서비스 단에서 수정 차단하지 않음 (정책 검토 필요)")
        void deletedMessage_notBlocked() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO deleted = messageOf(202, COMPANY_EMAIL, true, false);
            when(messageMapper.findById(202)).thenReturn(deleted, deleted);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());

            chatService.updateMessage(CONTRACT_ID, 202, "수정시도");

            verify(messageMapper).update(202, "수정시도");
        }
    }

    // ═══════════════════════════════════════════════════
    // getMessages (chatRoomId 기반 내부용)
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("getMessages (chatRoomId 기반)")
    class GetMessages {

        @Test
        @DisplayName("정상 - size null이면 기본 30개")
        void success_defaultSize() {
            when(messageMapper.findMessages(CHAT_ROOM_ID, null, 30)).thenReturn(List.of());

            chatService.getMessages(CHAT_ROOM_ID, null, null);

            verify(messageMapper).findMessages(CHAT_ROOM_ID, null, 30);
        }

        @Test
        @DisplayName("size 지정 시 해당 값 사용")
        void success_customSize() {
            when(messageMapper.findMessages(CHAT_ROOM_ID, 20, 50)).thenReturn(List.of());

            chatService.getMessages(CHAT_ROOM_ID, 20, 50);

            verify(messageMapper).findMessages(CHAT_ROOM_ID, 20, 50);
        }

        @Test
        @DisplayName("contractId 검증 없이 chatRoomId만으로 조회 (내부 호출 전용)")
        void noChatRoomValidation_directlyCallsMapper() {
            when(messageMapper.findMessages(9999, null, 30)).thenReturn(List.of());

            chatService.getMessages(9999, null, null);

            verify(messageMapper).findMessages(9999, null, 30);
            verify(chatRoomMapper, never()).findByContractId(any());
        }
    }
}