package com.ssafy.lancit.domain.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.global.enums.MessageType;
import com.ssafy.lancit.global.enums.NotificationType;

/**
 * ChatService 단위 테스트
 *
 * 전제:
 *  - SecurityUtil.getCurrentEmail() 은 static 메서드이므로 Mockito-inline mockStatic 사용
 *  - ChatService 는 messagingTemplate(SimpMessagingTemplate) 을 주입받아
 *    deleteMessage / updateMessage 시 "/sub/chat/{chatRoomId}" 로 브로드캐스트한다고 가정
 *    (현재 합의된 수정 방향 기준 테스트)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    @Mock private ChatRoomMapper chatRoomMapper;
    @Mock private MessageMapper messageMapper;
    @Mock private MessageFileMapper messageFileMapper;
    @Mock private NotificationService notificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private ChatService chatService;
    private MockedStatic<SecurityUtil> securityUtilMock;

    private static final Integer CHAT_ROOM_ID = 10;
    private static final Integer CONTRACT_ID = 1;
    private static final String COMPANY_EMAIL = "company@example.com";
    private static final String FREELANCER_EMAIL = "freelancer@example.com";

    @BeforeEach
    void setUp() {
        // ChatService(ChatRoomMapper, MessageMapper, MessageFileMapper,
        //             NotificationService, SimpMessagingTemplate)
        chatService = new ChatService(
                chatRoomMapper,
                messageMapper,
                messageFileMapper,
                notificationService,
                messagingTemplate
        );

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

    private MessageDTO messageOf(Integer messageId, String sender, boolean deleted, boolean updated) {
        return MessageDTO.builder()
                .messageId(messageId)
                .chatRoomId(CHAT_ROOM_ID)
                .senderEmail(sender)
                .messageType(MessageType.TEXT)
                .message("hello")
                .isDeleted(deleted)
                .isUpdated(updated)
                .build();
    }

    // ════════════════════════════════════════════════════
    // sendMessage
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("정상 - 회사가 보내면 프리랜서에게 CHAT 알림이 간다")
        void success_companyToFreelancer() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO saved = messageOf(100, COMPANY_EMAIL, false, false);
            when(messageMapper.findById(any())).thenReturn(saved);
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            MessageDTO result = chatService.sendMessage(CHAT_ROOM_ID, "hello");

            assertThat(result).isEqualTo(saved);

            ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
            verify(messageMapper).insert(captor.capture());
            assertThat(captor.getValue().getChatRoomId()).isEqualTo(CHAT_ROOM_ID);
            assertThat(captor.getValue().getSenderEmail()).isEqualTo(COMPANY_EMAIL);
            assertThat(captor.getValue().getMessage()).isEqualTo("hello");

            verify(notificationService).createNotification(
                    FREELANCER_EMAIL, NotificationType.CHAT, CONTRACT_ID);
        }

        @Test
        @DisplayName("정상 - 프리랜서가 보내면 회사에게 CHAT 알림이 간다")
        void success_freelancerToCompany() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);

            MessageDTO saved = messageOf(101, FREELANCER_EMAIL, false, false);
            when(messageMapper.findById(any())).thenReturn(saved);
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendMessage(CHAT_ROOM_ID, "안녕하세요");

            verify(notificationService).createNotification(
                    COMPANY_EMAIL, NotificationType.CHAT, CONTRACT_ID);
        }

        @Test
        @DisplayName("빈 문자열 메시지도 그대로 insert 된다 (서비스 단에서 내용 검증 없음)")
        void emptyContent_stillInserted() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(any())).thenReturn(messageOf(102, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendMessage(CHAT_ROOM_ID, "");

            ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
            verify(messageMapper).insert(captor.capture());
            assertThat(captor.getValue().getMessage()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 채팅방으로 보내면 알림 생성 단계에서 NPE 발생 (chatRoomMapper.findById == null)")
        void chatRoomNotFound_npeOnNotification() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(any())).thenReturn(messageOf(103, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(null);

            assertThatThrownBy(() -> chatService.sendMessage(CHAT_ROOM_ID, "hi"))
                    .isInstanceOf(NullPointerException.class);

            // insert는 이미 수행됨 - 메시지 자체는 저장되지만 알림에서 실패
            verify(messageMapper).insert(any());
        }
    }

    // ════════════════════════════════════════════════════
    // sendFileMessage
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("sendFileMessage")
    class SendFileMessage {

        @Test
        @DisplayName("정상 - 메시지 insert + messageFile insert + 알림 발송")
        void success() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO saved = MessageDTO.builder()
                    .messageId(200)
                    .chatRoomId(CHAT_ROOM_ID)
                    .senderEmail(COMPANY_EMAIL)
                    .messageType(MessageType.FILE)
                    .build();
            when(messageMapper.findById(any())).thenReturn(saved);
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            MessageDTO result = chatService.sendFileMessage(CHAT_ROOM_ID, 999);

            assertThat(result.getMessageType()).isEqualTo(MessageType.FILE);

            ArgumentCaptor<MessageDTO> msgCaptor = ArgumentCaptor.forClass(MessageDTO.class);
            verify(messageMapper).insert(msgCaptor.capture());
            assertThat(msgCaptor.getValue().getMessage()).isNull();

            // messageId는 insert 호출 시점에 DB가 채워주므로(Mockito mock에선 null) fileId만 확인
            verify(messageFileMapper).insert(any(), eq(999));

            verify(notificationService).createNotification(
                    FREELANCER_EMAIL, NotificationType.CHAT, CONTRACT_ID);
        }

        @Test
        @DisplayName("fileId가 null이어도 messageFileMapper.insert가 그대로 호출된다 (서비스 단 검증 없음)")
        void nullFileId_passthrough() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(any())).thenReturn(
                    MessageDTO.builder().messageId(201).chatRoomId(CHAT_ROOM_ID)
                            .senderEmail(COMPANY_EMAIL).messageType(MessageType.FILE).build());
            when(chatRoomMapper.findById(CHAT_ROOM_ID)).thenReturn(roomOf());

            chatService.sendFileMessage(CHAT_ROOM_ID, null);

            verify(messageFileMapper).insert(any(), eq((Integer) null));
        }
    }

    // ════════════════════════════════════════════════════
    // getMessagesByContractId
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("getMessagesByContractId")
    class GetMessagesByContractId {

        @Test
        @DisplayName("정상 - cursor 없으면 최신 30개 조회")
        void success_defaultSize() {
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());
            when(messageMapper.findMessages(CHAT_ROOM_ID, null, 30))
                    .thenReturn(List.of(messageOf(1, COMPANY_EMAIL, false, false)));

            List<MessageDTO> result = chatService.getMessagesByContractId(CONTRACT_ID, null, null);

            assertThat(result).hasSize(1);
            verify(messageMapper).findMessages(CHAT_ROOM_ID, null, 30);
        }

        @Test
        @DisplayName("size 파라미터가 있으면 그 값을 그대로 사용한다")
        void success_customSize() {
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());
            when(messageMapper.findMessages(CHAT_ROOM_ID, 50, 10)).thenReturn(List.of());

            chatService.getMessagesByContractId(CONTRACT_ID, 50, 10);

            verify(messageMapper).findMessages(CHAT_ROOM_ID, 50, 10);
        }

        @Test
        @DisplayName("채팅방이 없으면 NOT_FOUND")
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
        void emptyMessages() {
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());
            when(messageMapper.findMessages(CHAT_ROOM_ID, null, 30)).thenReturn(Collections.emptyList());

            List<MessageDTO> result = chatService.getMessagesByContractId(CONTRACT_ID, null, null);

            assertThat(result).isEmpty();
        }
    }

    // ════════════════════════════════════════════════════
    // deleteMessage
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("deleteMessage")
    class DeleteMessage {

        @Test
        @DisplayName("정상 - 본인 메시지 삭제 시 softDelete 후 /sub/chat/{chatRoomId} 로 브로드캐스트")
        void success_broadcasts() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO original = messageOf(300, COMPANY_EMAIL, false, false);
            MessageDTO afterDelete = messageOf(300, COMPANY_EMAIL, true, false);

            when(messageMapper.findById(300))
                    .thenReturn(original)   // 1차: 권한확인용
                    .thenReturn(afterDelete); // 2차: softDelete 후 재조회

            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());

            chatService.deleteMessage(CONTRACT_ID, 300);

            verify(messageMapper).softDelete(300);

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/sub/chat/" + CHAT_ROOM_ID), payloadCaptor.capture());
            assertThat(((MessageDTO) payloadCaptor.getValue()).isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 메시지면 NOT_FOUND, softDelete/브로드캐스트 호출 안됨")
        void messageNotFound_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(999)).thenReturn(null);

            assertThatThrownBy(() -> chatService.deleteMessage(CONTRACT_ID, 999))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(messageMapper, never()).softDelete(any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(MessageDTO.class));
        }

        @Test
        @DisplayName("타인의 메시지를 삭제하려 하면 FORBIDDEN")
        void notOwner_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);
            when(messageMapper.findById(300)).thenReturn(messageOf(300, COMPANY_EMAIL, false, false));

            assertThatThrownBy(() -> chatService.deleteMessage(CONTRACT_ID, 300))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(messageMapper, never()).softDelete(any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(MessageDTO.class));
        }

        @Test
        @DisplayName("contractId에 해당하는 채팅방이 없으면 INVALID_INPUT")
        void roomNotFound_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(300)).thenReturn(messageOf(300, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(null);

            assertThatThrownBy(() -> chatService.deleteMessage(CONTRACT_ID, 300))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(messageMapper, never()).softDelete(any());
        }

        @Test
        @DisplayName("메시지가 다른 채팅방(다른 contractId) 소속이면 INVALID_INPUT")
        void chatRoomMismatch_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO messageFromOtherRoom = MessageDTO.builder()
                    .messageId(300)
                    .chatRoomId(999) // 다른 채팅방
                    .senderEmail(COMPANY_EMAIL)
                    .build();
            when(messageMapper.findById(300)).thenReturn(messageFromOtherRoom);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf()); // chatRoomId=10

            assertThatThrownBy(() -> chatService.deleteMessage(CONTRACT_ID, 300))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(messageMapper, never()).softDelete(any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(MessageDTO.class));
        }

        @Test
        @DisplayName("이미 삭제된 메시지를 다시 삭제 요청해도 서비스 단에서는 차단하지 않고 그대로 진행된다")
        void alreadyDeleted_stillProceeds() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO alreadyDeleted = messageOf(300, COMPANY_EMAIL, true, false);
            when(messageMapper.findById(300)).thenReturn(alreadyDeleted);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());

            chatService.deleteMessage(CONTRACT_ID, 300);

            verify(messageMapper).softDelete(300);
            verify(messagingTemplate).convertAndSend(eq("/sub/chat/" + CHAT_ROOM_ID), any(MessageDTO.class));
        }
    }

    // ════════════════════════════════════════════════════
    // updateMessage
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateMessage")
    class UpdateMessage {

        @Test
        @DisplayName("정상 - 본인 메시지 수정 시 update 후 /sub/chat/{chatRoomId} 로 브로드캐스트")
        void success_broadcasts() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);

            MessageDTO original = messageOf(400, FREELANCER_EMAIL, false, false);
            MessageDTO afterUpdate = MessageDTO.builder()
                    .messageId(400).chatRoomId(CHAT_ROOM_ID).senderEmail(FREELANCER_EMAIL)
                    .messageType(MessageType.TEXT).message("수정된 내용")
                    .isDeleted(false).isUpdated(true).build();

            when(messageMapper.findById(400))
                    .thenReturn(original)
                    .thenReturn(afterUpdate);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());

            chatService.updateMessage(CONTRACT_ID, 400, "수정된 내용");

            verify(messageMapper).update(400, "수정된 내용");

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/sub/chat/" + CHAT_ROOM_ID), payloadCaptor.capture());
            MessageDTO sent = (MessageDTO) payloadCaptor.getValue();
            assertThat(sent.isUpdated()).isTrue();
            assertThat(sent.getMessage()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("존재하지 않는 메시지면 NOT_FOUND")
        void messageNotFound_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(999)).thenReturn(null);

            assertThatThrownBy(() -> chatService.updateMessage(CONTRACT_ID, 999, "new"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NOT_FOUND);

            verify(messageMapper, never()).update(any(), any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(MessageDTO.class));
        }

        @Test
        @DisplayName("타인의 메시지를 수정하려 하면 FORBIDDEN")
        void notOwner_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(FREELANCER_EMAIL);
            when(messageMapper.findById(400)).thenReturn(messageOf(400, COMPANY_EMAIL, false, false));

            assertThatThrownBy(() -> chatService.updateMessage(CONTRACT_ID, 400, "haha"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(messageMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("content가 null이면 그대로 mapper.update(messageId, null) 호출 (서비스 단 검증 없음)")
        void nullContent_passthrough() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO original = messageOf(401, COMPANY_EMAIL, false, false);
            when(messageMapper.findById(401)).thenReturn(original, original);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());

            chatService.updateMessage(CONTRACT_ID, 401, null);

            verify(messageMapper).update(401, null);
        }

        @Test
        @DisplayName("contractId에 해당하는 채팅방이 없으면 INVALID_INPUT, update/브로드캐스트 호출 안됨")
        void roomNotFound_throws() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);
            when(messageMapper.findById(400)).thenReturn(messageOf(400, COMPANY_EMAIL, false, false));
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(null);

            assertThatThrownBy(() -> chatService.updateMessage(CONTRACT_ID, 400, "new"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);

            verify(messageMapper, never()).update(any(), any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(MessageDTO.class));
        }

        @Test
        @DisplayName("이미 삭제된 메시지(isDeleted=true)도 서비스 단에서는 수정이 차단되지 않는다 (잠재적 정책 이슈)")
        void deletedMessage_stillEditable_potentialIssue() {
            securityUtilMock.when(SecurityUtil::getCurrentEmail).thenReturn(COMPANY_EMAIL);

            MessageDTO deleted = messageOf(402, COMPANY_EMAIL, true, false);
            when(messageMapper.findById(402)).thenReturn(deleted, deleted);
            when(chatRoomMapper.findByContractId(CONTRACT_ID)).thenReturn(roomOf());

            // 현재 구현은 isDeleted 여부를 검증하지 않으므로 예외 없이 통과한다.
            chatService.updateMessage(CONTRACT_ID, 402, "되돌리기 시도");

            verify(messageMapper).update(402, "되돌리기 시도");
        }
    }

    // ════════════════════════════════════════════════════
    // getMessages (ChatStompController 내부용 - chatRoomId 기반)
    // ════════════════════════════════════════════════════
    @Nested
    @DisplayName("getMessages (chatRoomId 기반)")
    class GetMessages {

        @Test
        @DisplayName("정상 - chatRoomId로 직접 조회, size 기본값 30")
        void success_defaultSize() {
            when(messageMapper.findMessages(CHAT_ROOM_ID, null, 30))
                    .thenReturn(List.of(messageOf(1, COMPANY_EMAIL, false, false)));

            List<MessageDTO> result = chatService.getMessages(CHAT_ROOM_ID, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("contractId 검증 없이 chatRoomId만으로 조회한다 (내부 호출용 - 외부 노출 시 주의)")
        void noContractValidation() {
            when(messageMapper.findMessages(999, null, 30)).thenReturn(List.of());

            // 존재하지 않을 수도 있는 chatRoomId(999)를 그대로 넘겨도 mapper 호출은 발생
            List<MessageDTO> result = chatService.getMessages(999, null, 30);

            assertThat(result).isEmpty();
            verify(messageMapper).findMessages(999, null, 30);
        }
    }

    // 헬퍼: Mockito ArgumentMatchers.anyString 정적 임포트 누락 방지용
    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}