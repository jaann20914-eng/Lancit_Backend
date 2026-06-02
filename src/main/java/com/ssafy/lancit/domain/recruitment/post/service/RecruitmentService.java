package com.ssafy.lancit.domain.recruitment.post.service;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.common.page.dto.PageResponse;
import com.ssafy.lancit.domain.recruitment.post.dto.RecruitmentDTO;
import com.ssafy.lancit.domain.recruitment.post.mapper.RecruitmentMapper;
import com.ssafy.lancit.global.enums.JobCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 공고문 CRUD - 프리랜서(조회), 회사(등록/조회)
// Redis, STOMP 직접 관련 없음
@Service
@RequiredArgsConstructor
public class RecruitmentService {

    private final RecruitmentMapper recruitmentMapper;

    // APPLY-01 프리랜서용 공고문 목록 조회 (업종/키워드 필터 + 페이지네이션)
    public PageResponse<RecruitmentDTO> getList(JobCategory jobCategory, String keyword,
                                                 PageRequest pageRequest) {
        // TODO 영은 [1]: List<RecruitmentDTO> list = recruitmentMapper.findAll(jobCategory, keyword, pageRequest)
        // TODO 영은 [2]: long total = recruitmentMapper.countAll(jobCategory, keyword)
        // TODO 영은 [3]: return PageResponse.of(list, total, pageRequest)
        List<RecruitmentDTO> list = recruitmentMapper.findAll(jobCategory, keyword, pageRequest);
        long total = recruitmentMapper.countAll(jobCategory, keyword);
        return PageResponse.of(list, total, pageRequest);
    }

    // CLI-APPLY-01 회사 내 공고문 목록 조회 (상태 필터 + 페이지네이션)
    public PageResponse<RecruitmentDTO> getMyList(String email, String status,
                                                   PageRequest pageRequest) {
        // TODO 영은 [1]: List<RecruitmentDTO> list = recruitmentMapper.findByCompany(email, status, pageRequest)
        // TODO 영은 [2]: long total = recruitmentMapper.countByCompany(email, status)
        // TODO 영은 [3]: return PageResponse.of(list, total, pageRequest)
        List<RecruitmentDTO> list = recruitmentMapper.findByCompany(email, status, pageRequest);
        long total = recruitmentMapper.countByCompany(email, status);
        return PageResponse.of(list, total, pageRequest);
    }

    // APPLY-02 공고문 상세 조회
    public RecruitmentDTO getOne(int recruitmentId) {
        // TODO 영은 [1]: RecruitmentDTO dto = recruitmentMapper.findById(recruitmentId)
        // TODO 영은 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 영은 [3]: return dto
        return null;
    }

    // CLI-APPLY-02 공고문 등록 (회사 전용)
    @Transactional
    public void create(RecruitmentDTO dto, String email) {
        // TODO 영은 [1]: dto.setEmail(email)
        // TODO 영은 [2]: recruitmentMapper.insert(dto)
    }
}