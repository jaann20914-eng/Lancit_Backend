package com.ssafy.lancit.domain.notification.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.notification.dto.NotificationDTO;
import com.ssafy.lancit.global.enums.NotificationType;

@Mapper
public interface NotificationMapper {

    // 알림 생성
    int insert(NotificationDTO dto);

    // 알림 목록 가져오기 
    List<NotificationDTO> findByReceiverEmail(
            String receiverEmail
    );

    // 단건 조회
    NotificationDTO findById(
            Integer notificationId
    );

    // 읽음 처리
    int markAsRead(
            Integer notificationId
    );

    // 특정 계약 알림 읽음 처리
    int markContractNotificationsAsRead(
            @Param("receiverEmail") String receiverEmail,
            @Param("contractId") Integer contractId
    );

    // 안읽은 알림 개수
    int countUnread(
            String receiverEmail
    );
    
    
    //특정 계약의 특정 타입 알림만 읽음 처리 (CONFIRM_FILE 드롭다운, CANCEL_REQUEST 파기확정, PROPOSAL 등)
    int markSpecificTypeAsRead(
            @Param("receiverEmail") String receiverEmail,
            @Param("contractId") Integer contractId,
            @Param("type") NotificationType type
    );

    // 특정 타입을 제외하고 계약 알림 읽음 처리 (상세페이지 진입 시 사용)
    int markContractNotificationsAsReadExcluding(
            @Param("receiverEmail") String receiverEmail,
            @Param("contractId") Integer contractId,
            @Param("excludeTypes") List<NotificationType> excludeTypes
    );
    
    
}