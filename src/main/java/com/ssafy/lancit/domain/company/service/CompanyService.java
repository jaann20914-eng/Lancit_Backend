package com.ssafy.lancit.domain.company.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.annotation.ContractGuard;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyService {
 
    private final CompanyMapper companyMapper;
    private final CategoryMapper categoryMapper;
    private final TaskMapper taskMapper;
 
    /** CLI-USER-03 마이페이지 조회 */
    public CompanyDTO getMe(String email) {
        // TODO 지원: companyMapper.findByEmail(email) → 없으면 NOT_FOUND 예외
        return null;
    }
 
    /** CLI-USER-04 마이페이지 수정 */
    @Transactional
    public void update(CompanyDTO dto) {
        // TODO 지원: companyMapper.update(dto)
    }
 
    /** CLI-USER-02 회원 탈퇴
     *  @ContractGuard → 진행 중 계약 있으면 ContractGuardAspect 에서 차단 */
    @ContractGuard
    @Transactional
    public void delete() {
        String email = SecurityUtil.getCurrentEmail();
        // TODO 지원: taskMapper.deleteByOwner(email, OwnerType.COMPANY)
        //   categoryMapper.deleteByOwner(email, OwnerType.COMPANY)
        //   companyMapper.delete(email) → File/Recruitment/Bookmark CASCADE 자동
    }
}