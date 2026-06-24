package com.ssafy.lancit.domain.notification.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.notification.dto.NotificationDTO;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.global.enums.NotificationType;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    // 알림 목록 조회
    @GetMapping
    public ApiResponse<List<NotificationDTO>> getNotifications() {

        String currentEmail = SecurityUtil.getCurrentEmail();
        return ApiResponse.ok(
                notificationService.getNotifications(currentEmail)
        );
    }


    // 특정 계약 관련 알림 읽음 처리 (계약 상세 진입 시)
    @PatchMapping("/contracts/{contractId}/read")
    public ApiResponse<Void> readNotifications(
            @PathVariable Integer contractId) {

        String currentEmail = SecurityUtil.getCurrentEmail();
        notificationService.markContractNotificationsAsRead(currentEmail, contractId);
        return ApiResponse.ok(null);
    }


    // 읽지 않은 알림 존재 여부 (네비게이션 🔴 표시용)
    @GetMapping("/unread-exists")
    public ApiResponse<Boolean> hasUnreadNotification() {
        String currentEmail = SecurityUtil.getCurrentEmail();
        boolean hasUnread = notificationService.countUnread(currentEmail) > 0;
        return ApiResponse.ok(hasUnread);
    }
    
	 // 컨펌파일 드롭다운 오픈 시 CONFIRM_FILE 알림 제거 (회사 전용)
	    @PatchMapping("/contracts/{contractId}/confirm-files/read")
	    public ApiResponse<Void> readConfirmFileNotification(
	            @PathVariable Integer contractId) {
	
	        String currentEmail = SecurityUtil.getCurrentEmail();
	        notificationService.markSpecificTypeAsRead(
	                currentEmail, contractId, NotificationType.CONFIRM_FILE);
	        return ApiResponse.ok(null);
	    }
	    
	    
	 // NotificationController.java에 추가
	    @PatchMapping("/contracts/{contractId}/types/read")
	    public ApiResponse<Void> readByTypes(
	            @PathVariable Integer contractId,
	            @RequestBody List<NotificationType> types) {
	        String currentEmail = SecurityUtil.getCurrentEmail();
	        notificationService.markTypeListAsRead(currentEmail, contractId, types);
	        return ApiResponse.ok(null);
	    }
	    
    // 필요하다면 컨트롤러에 추가
//    @PatchMapping("/{notificationId}/read")
//    public ApiResponse<Void> readOne(@PathVariable Integer notificationId) {
//        notificationService.markAsRead(notificationId);
//        return ApiResponse.ok(null);
//    }
}