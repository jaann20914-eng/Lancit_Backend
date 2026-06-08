package com.ssafy.lancit.domain.company.service;

import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.annotation.ContractGuard;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.file.dto.FileDTO;
import com.ssafy.lancit.domain.file.event.FileDeleteEvent;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.global.enums.FileParentType;
import com.ssafy.lancit.global.enums.OwnerType;

import lombok.RequiredArgsConstructor;

// 회사 마이페이지 조회 / 수정 / 탈퇴
// 프로필 사진은 FileController 에서 별도 처리 (프론트가 /api/files/{id}/url 별도 호출)
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyMapper companyMapper;
    private final CategoryMapper categoryMapper;
    private final TaskMapper taskMapper;
    private final FileMapper fileMapper;
    private final FileService fileService;   // 탈퇴 시 GCS + Redis 정리용
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    // CLI-USER-03 마이페이지 조회 - profileFileId 포함 반환 (Signed URL 은 프론트가 별도 호출)
    public CompanyDTO getMe(String email) {
    	CompanyDTO company = companyMapper.findByEmail(email);
    	if(company==null) throw new CustomException(ErrorCode.NOT_FOUND);
    	company.setPassword("");
        return company;
    }

    // CLI-USER-04 마이페이지 수정 - null 필드는 UPDATE 제외 (XML <if test> 처리)
    @Transactional
    public void update(CompanyDTO dto) {
        // [1] 기존 회사 조회
        CompanyDTO existing = companyMapper.findByEmail(dto.getEmail());
        if (existing == null) throw new CustomException(ErrorCode.NOT_FOUND);

        // [2] 프로필 사진 변경 시
        if (dto.getProfileFileId() != null) {
            // 기존 PROFILE 삭제
            if (existing.getProfileFileId() != null) {
                FileDTO oldFile = fileMapper.findById(existing.getProfileFileId());
                if (oldFile != null) {
                    fileService.delete(oldFile.getFileId());
                }
            }
            // TEMP → PROFILE 로 변경
            fileMapper.updateParentType(dto.getProfileFileId(), FileParentType.PROFILE);
        }

        // [3] 회사 정보 업데이트
        companyMapper.update(dto);
    }

    // CLI-USER-02 회원 탈퇴
    // - @ContractGuard → 진행 중 계약 있으면 차단
    // 삭제 순서:
    //   1. GCS + Redis 먼저 정리 (파일 목록 조회 → FileDeleteEvent 발행)
    //   2. Task, Category 앱 레벨 삭제
    //   3. companyMapper.delete() → DB CASCADE 로 나머지 자동 삭제
    //      (file, recruitment → application, bookmark, chatroom, message, proposal)
    @ContractGuard
    @Transactional
    public void delete() {
        String email = SecurityUtil.getCurrentEmail();

        // [1] GCS + Redis 정리 (CASCADE 전에 먼저 처리)
        List<FileDTO> files = fileMapper.findByCompanyEmail(email);
        for (FileDTO file : files) {
            eventPublisher.publishEvent(new FileDeleteEvent(file.getUploadPath()));
            cacheManager.getCache("signedUrl").evict(file.getFileId());
        }

        // [2] Task, Category 앱 레벨 삭제
        taskMapper.deleteByOwner(email, OwnerType.COMPANY);
        categoryMapper.deleteByOwner(email, OwnerType.COMPANY);

        // [3] Company 삭제 → DB CASCADE 자동 처리
        // File, Recruitment → Application, Bookmark, ChatRoom, Message, Proposal
        companyMapper.delete(email);
    }
}