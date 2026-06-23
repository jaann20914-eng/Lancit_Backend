package com.ssafy.lancit.domain.chat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.lancit.common.response.ApiResponse;
import com.ssafy.lancit.domain.chat.dto.MessageDTO;
import com.ssafy.lancit.domain.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contracts/{contractId}/messages")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    // 메시지 목록 조회 (무한스크롤, 기본 30개)
    @GetMapping
    public ApiResponse<List<MessageDTO>> getMessages(
            @PathVariable Integer contractId,
            @RequestParam(required = false) Integer cursor, //마지막 메세지 아이디
            @RequestParam(required = false, defaultValue = "30") Integer size) {

        return ApiResponse.ok( chatService.getMessagesByContractId(contractId, cursor, size));
    }


    // 메시지 삭제
    @DeleteMapping("/{messageId}")
    public ApiResponse<Void> deleteMessage(
            @PathVariable Integer contractId,
            @PathVariable Integer messageId) {

        chatService.deleteMessage(contractId, messageId);
        return ApiResponse.ok(null);
    }


    // 메시지 수정
    @PutMapping("/{messageId}")
    public ApiResponse<Void> updateMessage(
            @PathVariable Integer contractId,
            @PathVariable Integer messageId,
            @RequestBody Map<String, Object> request) {

        String content = (String) request.get("content");
        chatService.updateMessage(contractId, messageId, content);
        return ApiResponse.ok(null);
    }
}