package com.ssafy.lancit.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.ssafy.lancit.domain.notification.dto.NotificationDTO;
import com.ssafy.lancit.domain.notification.mapper.NotificationMapper;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.domain.notification.websocket.NotificationStompPublisher;
import com.ssafy.lancit.global.enums.NotificationType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock private NotificationMapper notificationMapper;
    @Mock private NotificationStompPublisher notificationStompPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private static final String EMAIL = "test@lancit.com";
    private static final String COMPANY_EMAIL = "company@lancit.com";
    private static final Integer CONTRACT_ID = 9;

    // ═══════════════════════════════════════════════════
    // createNotification
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("createNotification")
    class CreateNotification {

        @Test
        @DisplayName("정상 - DB insert 후 STOMP 실시간 push")
        void success() {
            notificationService.createNotification(EMAIL, NotificationType.CHAT, CONTRACT_ID);

            verify(notificationMapper).insert(any(NotificationDTO.class));
            verify(notificationStompPublisher).publish(any(NotificationDTO.class));
        }

        @Test
        @DisplayName("insert된 DTO가 올바른 receiverEmail, type, targetId를 가진다")
        void insertDto_hasCorrectFields() {
            notificationService.createNotification(EMAIL, NotificationType.PROPOSAL, CONTRACT_ID);

            ArgumentCaptor<NotificationDTO> captor = ArgumentCaptor.forClass(NotificationDTO.class);
            verify(notificationMapper).insert(captor.capture());

            NotificationDTO dto = captor.getValue();
            assertThat(dto.getReceiverEmail()).isEqualTo(EMAIL);
            assertThat(dto.getType()).isEqualTo(NotificationType.PROPOSAL);
            assertThat(dto.getTargetId()).isEqualTo(CONTRACT_ID);
        }

        @Test
        @DisplayName("STOMP publisher에 전달된 DTO가 insert된 것과 동일한 객체")
        void publishDto_sameAsInserted() {
            notificationService.createNotification(EMAIL, NotificationType.CONFIRM_FILE, CONTRACT_ID);

            ArgumentCaptor<NotificationDTO> insertCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
            ArgumentCaptor<NotificationDTO> publishCaptor = ArgumentCaptor.forClass(NotificationDTO.class);

            verify(notificationMapper).insert(insertCaptor.capture());
            verify(notificationStompPublisher).publish(publishCaptor.capture());

            assertThat(insertCaptor.getValue()).isSameAs(publishCaptor.getValue());
        }

        @Test
        @DisplayName("6가지 NotificationType 모두 insert+publish 호출됨")
        void allTypes_succeed() {
            for (NotificationType type : NotificationType.values()) {
                notificationService.createNotification(EMAIL, type, CONTRACT_ID);
            }
            verify(notificationMapper, times(NotificationType.values().length)).insert(any());
            verify(notificationStompPublisher, times(NotificationType.values().length)).publish(any());
        }

        @Test
        @DisplayName("targetId=null이어도 insert/publish 호출됨 (서비스 단 null 검증 없음)")
        void nullTargetId_passthrough() {
            notificationService.createNotification(EMAIL, NotificationType.CHAT, null);

            ArgumentCaptor<NotificationDTO> captor = ArgumentCaptor.forClass(NotificationDTO.class);
            verify(notificationMapper).insert(captor.capture());
            assertThat(captor.getValue().getTargetId()).isNull();
        }

        @Test
        @DisplayName("동일 receiverEmail에 여러 번 호출해도 각각 별도 insert/publish됨 (idempotent 아님)")
        void multipleCallsSameEmail_eachInserted() {
            notificationService.createNotification(EMAIL, NotificationType.CHAT, 1);
            notificationService.createNotification(EMAIL, NotificationType.CHAT, 2);
            notificationService.createNotification(EMAIL, NotificationType.CHAT, 3);

            verify(notificationMapper, times(3)).insert(any());
            verify(notificationStompPublisher, times(3)).publish(any());
        }
    }

    // ═══════════════════════════════════════════════════
    // getNotifications
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("정상 - receiverEmail로 알림 목록 조회")
        void success() {
            List<NotificationDTO> list = List.of(
                    NotificationDTO.builder()
                            .notificationId(2).receiverEmail(EMAIL)
                            .type(NotificationType.CHAT).targetId(CONTRACT_ID).isRead(false).build(),
                    NotificationDTO.builder()
                            .notificationId(1).receiverEmail(EMAIL)
                            .type(NotificationType.PROPOSAL).targetId(CONTRACT_ID).isRead(true).build()
            );
            when(notificationMapper.findByReceiverEmail(EMAIL)).thenReturn(list);

            List<NotificationDTO> result = notificationService.getNotifications(EMAIL);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getNotificationId()).isEqualTo(2);
        }

        @Test
        @DisplayName("알림이 없으면 빈 리스트 반환")
        void empty() {
            when(notificationMapper.findByReceiverEmail(EMAIL)).thenReturn(Collections.emptyList());

            List<NotificationDTO> result = notificationService.getNotifications(EMAIL);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다른 이메일의 알림은 조회되지 않는다 (mapper 호출 파라미터 확인)")
        void emailIsolation() {
            when(notificationMapper.findByReceiverEmail(EMAIL)).thenReturn(List.of());

            notificationService.getNotifications(EMAIL);

            verify(notificationMapper).findByReceiverEmail(EMAIL);
            verify(notificationMapper, never()).findByReceiverEmail(COMPANY_EMAIL);
        }
    }

    // ═══════════════════════════════════════════════════
    // countUnread
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("countUnread")
    class CountUnread {

        @Test
        @DisplayName("안 읽은 알림 개수 반환")
        void returnsCount() {
            when(notificationMapper.countUnread(EMAIL)).thenReturn(3);

            int result = notificationService.countUnread(EMAIL);

            assertThat(result).isEqualTo(3);
        }

        @Test
        @DisplayName("알림 없으면 0 반환")
        void noUnread_returnsZero() {
            when(notificationMapper.countUnread(EMAIL)).thenReturn(0);

            assertThat(notificationService.countUnread(EMAIL)).isZero();
        }
    }

    // ═══════════════════════════════════════════════════
    // markContractNotificationsAsRead
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("markContractNotificationsAsRead")
    class MarkContractNotificationsAsRead {

        @Test
        @DisplayName("CONFIRM_FILE, CONTRACT_CANCEL_REQUEST를 제외한 타입만 읽음처리")
        void excludes_confirmFile_and_cancelRequest() {
            notificationService.markContractNotificationsAsRead(EMAIL, CONTRACT_ID);

            ArgumentCaptor<List<NotificationType>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationMapper).markContractNotificationsAsReadExcluding(
                    eq(EMAIL), eq(CONTRACT_ID), captor.capture());

            assertThat(captor.getValue())
                    .containsExactlyInAnyOrder(
                            NotificationType.CONTRACT_CANCEL_REQUEST,
                            NotificationType.CONFIRM_FILE);
        }

        @Test
        @DisplayName("별도 트랜잭션(REQUIRES_NEW)으로 실행되므로 mapper 호출은 정상 위임됨")
        void delegatesToMapper() {
            notificationService.markContractNotificationsAsRead(EMAIL, CONTRACT_ID);

            verify(notificationMapper).markContractNotificationsAsReadExcluding(any(), any(), any());
        }

        @Test
        @DisplayName("동일 email+contractId로 여러 번 호출해도 mapper는 호출 횟수만큼 실행 (idempotent는 DB 책임)")
        void calledMultipleTimes_mapperCalledEachTime() {
            notificationService.markContractNotificationsAsRead(EMAIL, CONTRACT_ID);
            notificationService.markContractNotificationsAsRead(EMAIL, CONTRACT_ID);

            verify(notificationMapper, times(2))
                    .markContractNotificationsAsReadExcluding(any(), any(), any());
        }
    }

    // ═══════════════════════════════════════════════════
    // markSpecificTypeAsRead
    // ═══════════════════════════════════════════════════
    @Nested
    @DisplayName("markSpecificTypeAsRead")
    class MarkSpecificTypeAsRead {

        @Test
        @DisplayName("CONFIRM_FILE 타입 읽음처리 위임")
        void confirmFile() {
            notificationService.markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CONFIRM_FILE);

            verify(notificationMapper).markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CONFIRM_FILE);
        }

        @Test
        @DisplayName("CONTRACT_CANCEL_REQUEST 타입 읽음처리 위임")
        void cancelRequest() {
            notificationService.markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);

            verify(notificationMapper).markSpecificTypeAsRead(
                    EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);
        }

        @Test
        @DisplayName("PROPOSAL 타입 읽음처리 위임 (sendByCompany/sendByFreelancer 시 발송자 알림 클리어)")
        void proposal() {
            notificationService.markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.PROPOSAL);

            verify(notificationMapper).markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.PROPOSAL);
        }

        @Test
        @DisplayName("다른 email 호출 시 해당 email로 정확히 위임됨 (교차 오염 없음)")
        void emailIsolation() {
            notificationService.markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CHAT);
            notificationService.markSpecificTypeAsRead(COMPANY_EMAIL, CONTRACT_ID, NotificationType.CHAT);

            verify(notificationMapper).markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CHAT);
            verify(notificationMapper).markSpecificTypeAsRead(COMPANY_EMAIL, CONTRACT_ID, NotificationType.CHAT);
        }
    }
}