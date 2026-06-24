package com.ssafy.lancit.domain.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.domain.notification.dto.NotificationDTO;
import com.ssafy.lancit.domain.notification.mapper.NotificationMapper;
import com.ssafy.lancit.domain.notification.websocket.NotificationStompPublisher;
import com.ssafy.lancit.global.enums.NotificationType;

import lombok.RequiredArgsConstructor;

// 알림담당
// 알림 생성 : 읽음 처리
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationStompPublisher notificationStompPublisher;

    @Transactional
    public void createNotification(
            String receiverEmail,
            NotificationType type,
            Integer targetId ) {
        NotificationDTO dto = NotificationDTO.builder()
                        					 .receiverEmail(receiverEmail) 
                        					 .type(type)
                        					 .targetId(targetId)
                        					 .build();

        notificationMapper.insert(dto); // 알림 관련 db에 삽입
        notificationStompPublisher.publish(dto);// db저장후 스톰프로실시간 푸시
    }

    
    //알림 목록 가져오기
    public List<NotificationDTO> getNotifications( String receiverEmail ) {
        return notificationMapper.findByReceiverEmail(receiverEmail);
    }

    
    // 알림 읽음 처리
//    @Transactional
//    public void markAsRead( Integer notificationId ) {
//        notificationMapper.markAsRead(notificationId);
//    }

    
	 // 계약 상세페이지 진입 시 알림 읽음 처리
	 // - CONTRACT_CANCEL_REQUEST: 실제 파기 확정 시점에만 제거 (여기선 제외)
	 // - CONFIRM_FILE: 드롭다운 오픈 시점에만 제거 (여기선 제외)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markContractNotificationsAsRead(String receiverEmail, Integer contractId) {
        notificationMapper.markContractNotificationsAsReadExcluding(
                receiverEmail,
                contractId,
                List.of(NotificationType.CONTRACT_CANCEL_REQUEST, NotificationType.CONFIRM_FILE)
        );
    }
    
	// 특정 타입 알림만 읽음 처리 (CONFIRM_FILE 드롭다운, 파기확정, PROPOSAL 클리어 등에서 사용)
	 @Transactional
	 public void markSpecificTypeAsRead(String receiverEmail, Integer contractId, NotificationType type) {
	     notificationMapper.markSpecificTypeAsRead(receiverEmail, contractId, type);
	 }

    
    // 안 읽은 알림 갯수
    public int countUnread( String receiverEmail) {
        return notificationMapper.countUnread(receiverEmail);
    }
    
    
 // NotificationService.java에 추가
    @Transactional
    public void markTypeListAsRead(String receiverEmail, Integer contractId, List<NotificationType> types) {
        for (NotificationType type : types) {
            notificationMapper.markSpecificTypeAsRead(receiverEmail, contractId, type);
        }
    }
}