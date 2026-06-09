package com.ssafy.lancit.domain.user.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.lancit.common.page.dto.PageRequest;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.global.enums.JobCategory;

@Mapper
public interface UserMapper {

    //이메일로 유저 디티오 찾아오기
    UserDTO findByEmail(String email);

    //회원가입 시 사용
    void insert(UserDTO dto);

    //회원정보 수정
    void update(UserDTO dto);

    //회원 소프트 탈퇴 
    void softDelete(String email);


    //회원가입 이메일 중복 체크 시 사용
    boolean existsByEmail(String email);

    //비밀번호 업데이트
    void updatePassword(@Param("email") String email, @Param("password") String password);


	
	//프리랜서 리스트 가져오기
    List<UserDTO> searchFreelancers(
    	    @Param("keyword") String keyword,
    	    @Param("jobCategory") JobCategory jobCategory,
    	    @Param("bookmarked") boolean bookmarked,
    	    @Param("companyEmail") String companyEmail,
    	    @Param("sort") String sort,
    	    @Param("direction") String direction,  // 추가
    	    @Param("offset") int offset,
    	    @Param("size") int size);
    
    // 프리랜서 카운트 가져오기
    long countFreelancers(
    	    @Param("keyword") String keyword,
    	    @Param("jobCategory") JobCategory jobCategory,
    	    @Param("bookmarked") boolean bookmarked,
    	    @Param("companyEmail") String companyEmail);
    
}