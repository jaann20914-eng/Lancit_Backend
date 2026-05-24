package com.ssafy.lancit.domain.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.annotation.ContractGuard;
import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import com.ssafy.lancit.global.enums.OwnerType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TaskMapper taskMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * USER-03 마이페이지 조회
     *
     * TODO 지원 [1]: userMapper.findByEmail(email) 호출
     * TODO 지원 [2]: null 이면 throw new CustomException(ErrorCode.NOT_FOUND)
     * TODO 지원 [3]: 조회된 UserDTO 반환
     */
    public UserDTO getMe(String email) {
        // TODO 지원 [1] ~ [3] 구현
        return null;
    }

    /**
     * USER-04 마이페이지 수정
     * - 비밀번호 변경 시 BCrypt 암호화 처리
     *
     * TODO 지원 [1]: dto.getPassword() 가 null 이 아니면 (비밀번호 변경 요청)
     *               - dto.setPassword(passwordEncoder.encode(dto.getPassword()))
     * TODO 지원 [2]: userMapper.update(dto) 호출
     *               - XML 에서 null 인 필드는 UPDATE 제외 (<if test> 사용)
     */
    @Transactional
    public void update(UserDTO dto) {
        // TODO 지원 [1] ~ [2] 구현
    }

    /**
     * USER-02 회원 탈퇴
     * - @ContractGuard → ContractGuardAspect 에서 진행 중 계약 있으면 차단
     * - Category, Task 는 FK 없어서 앱 레벨에서 직접 삭제
     * - User 삭제 시 DB CASCADE 자동 삭제 목록
     *   File, Portfolio → PortfolioPermission,
     *   RecruitmentApplication → PortfolioPermission,
     *   Bookmark, ChatRoom → Message, Proposal
     *
     * TODO 지원 [1]: taskMapper.deleteByOwner(email, OwnerType.USER) 호출
     * TODO 지원 [2]: categoryMapper.deleteByOwner(email, OwnerType.USER) 호출
     * TODO 지원 [3]: userMapper.delete(email) 호출 → CASCADE 자동 처리
     */
    @ContractGuard
    @Transactional
    public void delete() {
        String email = SecurityUtil.getCurrentEmail();
        // TODO 지원 [1] ~ [3] 구현
    }
}