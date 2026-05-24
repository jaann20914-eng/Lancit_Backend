package com.ssafy.lancit.domain.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.user.dto.UserDTO;

@Mapper
public interface UserMapper {

    // TODO 지원: UserMapper.xml → SELECT * FROM user WHERE email = #{email}
    UserDTO findByEmail(String email);

    // TODO 지원: UserMapper.xml → INSERT INTO user (email, password, name, phone, ...)
    //            회원가입 시 사용
    void insert(UserDTO dto);

    // TODO 지원: UserMapper.xml → UPDATE user SET ...
    //            null 인 필드는 업데이트 제외 → <if test="xxx != null"> 사용
    //            WHERE email = #{email}
    void update(UserDTO dto);

    // TODO 지원: UserMapper.xml → DELETE FROM user WHERE email = #{email}
    void delete(String email);

    // TODO 지원: UserMapper.xml → SELECT COUNT(*) > 0 FROM user WHERE email = #{email}
    //            회원가입 이메일 중복 체크 시 사용
    boolean existsByEmail(String email);

    // TODO 지원: UserMapper.xml → UPDATE user SET password = #{password}
    //            WHERE email = #{email}
    //            비밀번호 찾기(AUTH-05) 시 사용
    void updatePassword(@Param("email") String email, @Param("password") String password);
}