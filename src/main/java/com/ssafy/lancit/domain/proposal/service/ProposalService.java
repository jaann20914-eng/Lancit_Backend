package com.ssafy.lancit.domain.proposal.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProposalService {
 
    public Object getList(Object... args) {
        // TODO 지원: getList(email, role)
        return null;
    }
 
    public Object getOne(Object... args) {
        // TODO 지원: getOne(proposalId)
        return null;
    }
 
    public Object send(Object... args) {
        // TODO 지원: send(dto, companyEmail)
        return null;
    }
 
    public Object accept(Object... args) {
        // TODO 지원: accept(proposalId, freelancerEmail) → Proposal ACCEPTED + Contract 삽입 + ChatRoom 삽입 (트랜잭션)
        return null;
    }
 
    public Object reject(Object... args) {
        // TODO 지원: reject(proposalId, freelancerEmail)
        return null;
    }
}
