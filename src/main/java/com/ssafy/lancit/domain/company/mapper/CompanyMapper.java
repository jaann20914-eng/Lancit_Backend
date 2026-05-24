package com.ssafy.lancit.domain.company.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.company.dto.CompanyDTO;

@Mapper
public interface CompanyMapper {

    // TODO 지원: CompanyMapper.xml → SELECT * FROM company WHERE email = #{email}
    CompanyDTO findByEmail(String email);

    // TODO 지원: CompanyMapper.xml → INSERT INTO company (email, password, name, company_name, ...)
    //            회원가입 시 사용
    void insert(CompanyDTO dto);

    // TODO 지원: CompanyMapper.xml → UPDATE company SET ...
    //            null 인 필드는 업데이트 제외 → <if test="xxx != null"> 사용
    //            WHERE email = #{email}
    void update(CompanyDTO dto);

    // TODO 지원: CompanyMapper.xml → DELETE FROM company WHERE email = #{email}
    void delete(String email);

    // TODO 지원: CompanyMapper.xml → SELECT COUNT(*) > 0 FROM company WHERE email = #{email}
    //            회원가입 이메일 중복 체크 시 사용
    boolean existsByEmail(String email);

    // TODO 지원: CompanyMapper.xml → UPDATE company SET password = #{password}
    //            WHERE email = #{email}
    //            비밀번호 찾기(AUTH-05) 시 사용
    void updatePassword(@Param("email") String email, @Param("password") String password);
}