package com.ssafy.lancit.domain.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.user.dto.UserDTO;

@Mapper
public interface UserMapper {

    //이메일로 유저 디티오 찾아오기
    UserDTO findByEmail(String email);

    //회원가입 시 사용
    void insert(UserDTO dto);

    //회원정보 수정
    void update(UserDTO dto);

    //회원 탈퇴 
    void delete(String email);


    //회원가입 이메일 중복 체크 시 사용
    boolean existsByEmail(String email);

    //비밀번호 업데이트
    void updatePassword(@Param("email") String email, @Param("password") String password);
    
 // TODO 지원
    List<UserDTO> searchFreelancers(@Param("name") String name,
            @Param("jobCategory") String jobCategory,
            @Param("pageRequest") PageRequest pageRequest);
 // TODO 지원
	long countFreelancers(@Param("name") String name, @Param("jobCategory") String jobCategory);
    
}