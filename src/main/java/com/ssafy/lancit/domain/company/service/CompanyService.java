package com.ssafy.lancit.domain.company.service;

import com.ssafy.lancit.common.annotation.ContractGuard;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.file.mapper.FileMapper;
import com.ssafy.lancit.domain.file.service.FileService;
import com.ssafy.lancit.global.enums.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회사 마이페이지 조회 / 수정 / 탈퇴
// 프로필 사진은 FileController 에서 별도 처리 (프론트가 /api/files/{id}/url 별도 호출)
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyMapper companyMapper;
    private final CategoryMapper categoryMapper;
    private final TaskMapper taskMapper;
    private final FileMapper fileMapper;
    private final FileService fileService;   // ★ 탈퇴 시 GCS + Redis 정리용
    private final PasswordEncoder passwordEncoder;

    // CLI-USER-03 마이페이지 조회 - profileFileId 포함 반환 (Signed URL 은 프론트가 별도 호출)
    public CompanyDTO getMe(String email) {
        // TODO 지원 [1]: CompanyDTO dto = companyMapper.findByEmail(email)
        // TODO 지원 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
        // TODO 지원 [3]: return dto
        return null;
    }

    // CLI-USER-04 마이페이지 수정 - null 필드는 UPDATE 제외 (XML <if test> 처리)
    @Transactional
    public void update(CompanyDTO dto) {
        // TODO 지원 [1]: dto.getPassword() != null 이면 BCrypt 암호화
        //               dto.setPassword(passwordEncoder.encode(dto.getPassword()))
        // TODO 지원 [2]: companyMapper.update(dto)
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

        // TODO 지원 [1]: ★ GCS + Redis 정리 (CASCADE 전에 먼저 처리)
        //               List<FileDTO> files = fileMapper.findByCompanyEmail(email)
        //               files.forEach(file -> fileService.delete(file.getFileId()))
        //               → FileDeleteEvent 발행 → 커밋 후 GCS 삭제 + Redis @CacheEvict

        // TODO 지원 [2]: taskMapper.deleteByOwner(email, OwnerType.COMPANY)
        // TODO 지원 [3]: categoryMapper.deleteByOwner(email, OwnerType.COMPANY)
        // TODO 지원 [4]: companyMapper.delete(email) → DB CASCADE 자동 처리
    }
}