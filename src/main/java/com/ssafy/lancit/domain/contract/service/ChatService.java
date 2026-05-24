package com.ssafy.lancit.domain.contract.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {
 
    public Object getMessages(Object... args) {
        // TODO 지원: getMessages(chatRoomId)
        return null;
    }
 
    public Object save(Object... args) {
        // TODO 지원: save(dto)
        return null;
    }
 
    public Object update(Object... args) {
        // TODO 지원: update(dto) → isUpdated=true
        return null;
    }
 
    public Object softDelete(Object... args) {
        // TODO 지원: softDelete(dto) → isDeleted=true
        return null;
    }
}
