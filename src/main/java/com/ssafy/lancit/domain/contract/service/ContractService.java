package com.ssafy.lancit.domain.contract.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContractService {
 
    public Object getList(Object... args) {
        // TODO 지원: getList(email, role, status, keyword)
        return null;
    }
 
    public Object getOne(Object... args) {
        // TODO 지원: getOne(contractId)
        return null;
    }
 
    public Object createOrUpdate(Object... args) {
        // TODO 지원: createOrUpdate(dto, companyEmail) → status 흐름 관리
        return null;
    }
 
    public Object accept(Object... args) {
        // TODO 지원: accept(contractId, dto, freelancerEmail) → status=IN_PROGRESS + freelancerSignFileId 수정
        return null;
    }
 
    public Object cancel(Object... args) {
        // TODO 지원: cancel(contractId, email) → status=CANCELLED
        return null;
    }
 
    public Object getChatRoom(Object... args) {
        // TODO 지원: getChatRoom(contractId)
        return null;
    }
}
