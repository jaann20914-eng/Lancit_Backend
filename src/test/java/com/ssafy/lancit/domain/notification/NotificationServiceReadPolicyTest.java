package com.ssafy.lancit.domain.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

import com.ssafy.lancit.domain.notification.mapper.NotificationMapper;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.domain.notification.websocket.NotificationStompPublisher;
import com.ssafy.lancit.global.enums.NotificationType;

/**
 * NotificationService 추가 테스트
 * - markContractNotificationsAsRead: CONFIRM_FILE / CONTRACT_CANCEL_REQUEST 제외 위임 확인
 * - markSpecificTypeAsRead: 타입 지정 읽음 처리 위임 확인
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceReadPolicyTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationStompPublisher stompPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private static final String EMAIL = "user@example.com";
    private static final Integer CONTRACT_ID = 1;

    @Nested
    @DisplayName("markContractNotificationsAsRead")
    class MarkContractNotificationsAsRead {

        @Test
        @DisplayName("CONFIRM_FILE, CONTRACT_CANCEL_REQUEST 를 제외한 나머지 타입만 읽음 처리한다")
        void excludesConfirmFileAndCancelRequest() {
            notificationService.markContractNotificationsAsRead(EMAIL, CONTRACT_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<NotificationType>> excludeCaptor = ArgumentCaptor.forClass(List.class);

            verify(notificationMapper, times(1))
                    .markContractNotificationsAsReadExcluding(eq(EMAIL), eq(CONTRACT_ID), excludeCaptor.capture());

            List<NotificationType> excluded = excludeCaptor.getValue();
            org.assertj.core.api.Assertions.assertThat(excluded)
                    .containsExactlyInAnyOrder(
                            NotificationType.CONTRACT_CANCEL_REQUEST,
                            NotificationType.CONFIRM_FILE
                    );

            // 구버전 메서드(타입 구분 없이 전체 읽음처리)는 더 이상 호출되지 않아야 한다
            verify(notificationMapper, never()).markContractNotificationsAsRead(any(), any());
        }

        @Test
        @DisplayName("contractId가 null이어도 그대로 위임한다")
        void nullContractId_passthrough() {
            notificationService.markContractNotificationsAsRead(EMAIL, null);

            verify(notificationMapper).markContractNotificationsAsReadExcluding(
                    eq(EMAIL), eq((Integer) null), any());
        }

        @Test
        @DisplayName("receiverEmail이 null이어도 그대로 위임한다")
        void nullEmail_passthrough() {
            notificationService.markContractNotificationsAsRead(null, CONTRACT_ID);

            verify(notificationMapper).markContractNotificationsAsReadExcluding(
                    eq((String) null), eq(CONTRACT_ID), any());
        }
    }

    @Nested
    @DisplayName("markSpecificTypeAsRead")
    class MarkSpecificTypeAsRead {

        @Test
        @DisplayName("CONFIRM_FILE 타입으로 호출하면 mapper에 그대로 전달된다 (드롭다운 오픈 시)")
        void confirmFile() {
            notificationService.markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CONFIRM_FILE);

            verify(notificationMapper).markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CONFIRM_FILE);
        }

        @Test
        @DisplayName("CONTRACT_CANCEL_REQUEST 타입으로 호출하면 mapper에 그대로 전달된다 (파기 확정 시)")
        void cancelRequest() {
            notificationService.markSpecificTypeAsRead(
                    EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);

            verify(notificationMapper).markSpecificTypeAsRead(
                    EMAIL, CONTRACT_ID, NotificationType.CONTRACT_CANCEL_REQUEST);
        }

        @Test
        @DisplayName("PROPOSAL 타입으로 호출하면 mapper에 그대로 전달된다 (발송 시 발송자측 알림 클리어)")
        void proposal() {
            notificationService.markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.PROPOSAL);

            verify(notificationMapper).markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.PROPOSAL);
        }

        @Test
        @DisplayName("동일 type+email+contractId로 두 번 호출해도 mapper는 두 번 모두 호출된다 (idempotent 여부는 mapper/DB 책임)")
        void calledTwice_bothInvoked() {
            notificationService.markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CONFIRM_FILE);
            notificationService.markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CONFIRM_FILE);

            verify(notificationMapper, times(2))
                    .markSpecificTypeAsRead(EMAIL, CONTRACT_ID, NotificationType.CONFIRM_FILE);
        }
    }
}