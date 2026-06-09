package com.ssafy.lancit.domain.company.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.global.enums.JobCategory;

@Mapper
public interface CompanyMapper {

    // 이메일로 컴패니디티오 가져오기
    CompanyDTO findByEmail(String email);

    // 회원가입 시 사용
    void insert(CompanyDTO dto);
    
    //회원가입 이메일 중복 체크 시 사용
    boolean existsByEmail(String email);

    //회사 정보 수정
    void update(CompanyDTO dto);

    //회원 탈퇴
    void softDelete(String email);

    // 비밀번호 업데이트
    void updatePassword(@Param("email") String email, @Param("password") String password);
    
    //회사 잡 카테고리 가져오기 
    JobCategory getCompanyJobCategory(String email);
    
}