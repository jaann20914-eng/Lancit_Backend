package com.ssafy.lancit.domain.chat.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.chat.dto.ChatRoomDTO;
import com.ssafy.lancit.domain.chat.dto.MessageDTO;
import com.ssafy.lancit.domain.chat.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.chat.mapper.MessageFileMapper;
import com.ssafy.lancit.domain.chat.mapper.MessageMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.notification.service.NotificationService;
import com.ssafy.lancit.global.enums.MessageType;
import com.ssafy.lancit.global.enums.NotificationType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomMapper chatRoomMapper;
    private final MessageMapper messageMapper;
    private final MessageFileMapper messageFileMapper;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final FileService fileService;

    // 메시지 발송
    @Transactional
    public MessageDTO sendMessage(Integer chatRoomId, String content,  String senderEmail, MessageType type) {

        //db에 집어넣기
        MessageDTO dto = MessageDTO.builder()
                					.chatRoomId(chatRoomId)
                					.senderEmail(senderEmail)
                					.message(content)
                					.messageType(type)
                					.build();
        messageMapper.insert(dto); 

        //메세지 알림 발송
        MessageDTO message = messageMapper.findById(dto.getMessageId());
        createChatNotification(chatRoomId, senderEmail);
        return message;
    }


    // 파일 메시지 발송
    @Transactional
    public MessageDTO sendFileMessage(Integer chatRoomId, Integer fileId,String senderEmail, MessageType type) {

       //db에 집어넣기
        MessageDTO dto = MessageDTO.builder()
                				    .chatRoomId(chatRoomId)
                				    .senderEmail(senderEmail)
                					.message("")
                					.messageType(type)
                					.build();
        messageMapper.insert(dto);
        messageFileMapper.insert(dto.getMessageId(), fileId);
        
        //메세지 알림 발송
        MessageDTO message = messageMapper.findById(dto.getMessageId());
        createChatNotification(chatRoomId, senderEmail);
        return message;
    }


    // 메시지 목록 조회 - contractId 기반 : ChatController 용(무한스크롤, 기본 30개)
    public List<MessageDTO> getMessagesByContractId( Integer contractId,
    												 Integer lastMessageId, //마지막 메세지 아이디
    												 Integer size) {
    	
        ChatRoomDTO room = chatRoomMapper.findByContractId(contractId);
        if (room == null) { throw new CustomException(ErrorCode.NOT_FOUND);}

        int pageSize = (size != null) ? size : 30;
        return messageMapper.findMessages(room.getChatRoomId(), lastMessageId, pageSize);
    }


    // 메시지 삭제 (본인만 가능)
    @Transactional
    public void deleteMessage(Integer contractId, Integer messageId) {

        String currentEmail = SecurityUtil.getCurrentEmail();
        
        //메세지 아이디로 메세지 dto 가져오기
        MessageDTO message = messageMapper.findById(messageId);
        if (message == null) { throw new CustomException(ErrorCode.NOT_FOUND);} // 메세지 아이디 없음 예외
        if (!currentEmail.equals(message.getSenderEmail())) { throw new CustomException(ErrorCode.FORBIDDEN);} // 메세지 소유자가 아님 예외

        // contractId -> chatRoomId 일치 확인  TODO 지원: 아래 내용 필요 없을수도 있음
        ChatRoomDTO room = chatRoomMapper.findByContractId(contractId);
        if (room == null || !room.getChatRoomId().equals(message.getChatRoomId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 첨부파일 실제 삭제 (GCS + file 테이블 + message_file 테이블)
        List<Integer> fileIds = messageFileMapper.findFileIdsByMessageId(messageId);
        for (Integer fileId : fileIds) {
            fileService.deleteBySystem(fileId);
        }
        messageFileMapper.deleteByMessageId(messageId);
        
        // 소프트 델리트
        messageMapper.softDelete(messageId);
        
        // 실시간 반영 - 삭제된 메시지 다시 조회 후 브로드캐스트
        MessageDTO updated = messageMapper.findById(messageId);
        messagingTemplate.convertAndSend(
                "/sub/chat/" + room.getChatRoomId(),
                updated
        );
    }


    // 메시지 수정 (본인만 가능)
    @Transactional
    public void updateMessage(Integer contractId, Integer messageId, String content) {

        String currentEmail = SecurityUtil.getCurrentEmail();
        
        //메세지 아이디로 메세지 dto 가져오기
        MessageDTO message = messageMapper.findById(messageId);
        if (message == null) { throw new CustomException(ErrorCode.NOT_FOUND); }
        if (!currentEmail.equals(message.getSenderEmail())) {throw new CustomException(ErrorCode.FORBIDDEN); }

        // contractId -> chatRoomId 일치 확인  TODO 지원: 아래 내용 필요 없을수도 있음
        ChatRoomDTO room = chatRoomMapper.findByContractId(contractId);
        if (room == null || !room.getChatRoomId().equals(message.getChatRoomId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        //소프트 업데이트
        messageMapper.update(messageId, content);
        
        // 실시간 반영 - 수정된 메시지 다시 조회 후 브로드캐스트
        MessageDTO updated = messageMapper.findById(messageId);
        messagingTemplate.convertAndSend(
                "/sub/chat/" + room.getChatRoomId(),
                updated
        );
    }
    
    
    
    //=====================================================내부적으로 필요한 메서드


    // 채팅 알림 생성 (내부용)
    private void createChatNotification(Integer chatRoomId, String senderEmail) {

        ChatRoomDTO room = chatRoomMapper.findById(chatRoomId);

        String receiverEmail = senderEmail.equals(room.getCompanyEmail())
                ? room.getFreelancerEmail()
                : room.getCompanyEmail();

        notificationService.createNotification( // 알림발송
                receiverEmail,
                NotificationType.CHAT,
                room.getContractId()
        );
    }
    
    // 메시지 목록 조회 - chatRoomId 기반 (ChatStompController 내부용)
    public List<MessageDTO> getMessages(
            Integer chatRoomId,
            Integer lastMessageId,
            Integer size) {

        int pageSize = (size != null) ? size : 30;
        return messageMapper.findMessages(chatRoomId, lastMessageId, pageSize);
    }
    
}