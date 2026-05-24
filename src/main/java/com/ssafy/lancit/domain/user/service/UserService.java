package com.ssafy.lancit.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.annotation.ContractGuard;
import com.ssafy.lancit.common.util.SecurityUtil;
import com.ssafy.lancit.domain.calendar.category.mapper.CategoryMapper;
import com.ssafy.lancit.domain.calendar.task.mapper.TaskMapper;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
 
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TaskMapper taskMapper;
 
    /** USER-03 마이페이지 조회 */
    public UserDTO getMe(String email) {
        // TODO 지원: userMapper.findByEmail(email) → 없으면 NOT_FOUND 예외
        return null;
    }
 
    /** USER-04 마이페이지 수정 */
    @Transactional
    public void update(UserDTO dto) {
        // TODO 지원: 비밀번호 변경 시 BCrypt 암호화
        //   프로필 이미지 변경 시 기존 파일 삭제 이벤트 발행
        //   userMapper.update(dto)
    }
 
    /** USER-02 회원 탈퇴
     *  @ContractGuard → 진행 중 계약 있으면 ContractGuardAspect 에서 차단 */
    @ContractGuard
    @Transactional
    public void delete() {
        String email = SecurityUtil.getCurrentEmail();
        // TODO 지원: taskMapper.deleteByOwner(email, OwnerType.USER) 앱 레벨 삭제
        //   categoryMapper.deleteByOwner(email, OwnerType.USER) 앱 레벨 삭제
        //   userMapper.delete(email) → File/Portfolio/Application/Bookmark CASCADE 자동
    }
}
