package com.ssafy.lancit.domain.auth.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.jwt.JwtTokenProvider;
import com.ssafy.lancit.common.util.BusinessNumberValidator;
import com.ssafy.lancit.domain.auth.dto.LoginDTO;
import com.ssafy.lancit.domain.auth.dto.SignupDTO;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.contract.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

// 회원가입 / 로그인 / 비밀번호 변경 / 로그아웃 비즈니스 로직
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final ChatRoomMapper chatRoomMapper;   // 로그인 시 chatRoomIds 조회용
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate; 
    private final BusinessNumberValidator businessNumberValidator;

    //회원가입 - 이메일 중복 확인 → 비밀번호 암호화 → role 분기 INSERT
    @Transactional
    public void signup(SignupDTO dto) {
    	//이메일 인증 유효한지 먼저 확인
    	String key = MailService.getVerifiedKey("signup", dto.getEmail());
    	String verified = redisTemplate.opsForValue().get(key);
        if (verified == null) throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED); 
    	
    	
        String role = dto.getRole();
        if (!"company".equals(role) && !"user".equals(role)) {throw new CustomException(ErrorCode.INVALID_ROLE);}
        
        boolean companyExists = companyMapper.existsByEmail(dto.getEmail());
        boolean userExists = userMapper.existsByEmail(dto.getEmail());
        //user로 가입한 이메일 회사걸로 가입 불가능하도록
        if (companyExists || userExists) {throw new CustomException(ErrorCode.DUPLICATE_EMAIL);} 

        String encodedPassword =passwordEncoder.encode(dto.getPassword());// 비번 암호화
        if ("company".equals(role)) {

            // 사업자 번호 있으면 백엔드에서 한 번 더 검증
            boolean verifiedBussinessNumber = false;
            if (dto.getBusinessNumber() != null && !dto.getBusinessNumber().isEmpty()) {
            	verifiedBussinessNumber = businessNumberValidator.validate(dto.getBusinessNumber());
                if (!verifiedBussinessNumber) {
                    throw new CustomException(ErrorCode.BUSINESS_API_ERROR);
                }
            }

            CompanyDTO companyDTO = CompanyDTO.builder()
                    .email(dto.getEmail())
                    .password(encodedPassword)
                    .name(dto.getName())
                    .companyName(dto.getCompanyName())
                    .phone(dto.getPhone())
                    .jobCategory(dto.getJobCategory())
                    .pushable(dto.isPushable())
                    .businessNumber(dto.getBusinessNumber())
                    .businessNumberVerified(verifiedBussinessNumber) 
                    .build();
            companyMapper.insert(companyDTO);
            
        } else if("user".equals(role)){
            UserDTO userDTO = UserDTO.builder()
                    .email(dto.getEmail())
                    .password(encodedPassword)
                    .name(dto.getName())
                    .phone(dto.getPhone())
                    .jobCategory(dto.getJobCategory())
                    .pushable(dto.isPushable())
                    .build();
            userMapper.insert(userDTO);
        }
        
        // 회원가입 완료 후 인증 키 삭제
        redisTemplate.delete(MailService.getVerifiedKey("signup", dto.getEmail()));
    }

    
    
    //AUTH-04 로그인 :이메일 조회 → 비밀번호 검증 → JWT 발급 → chatRoomIds 조회
    //STOMP: chatRoomIds 포함해줘야 함 → 프론트가 로그인 직후 WebSocket 연결 시
    // 1. /sub/notification/{email} 구독 (알림용)
    // 2. /sub/chat/{chatRoomId} 구독 (진행중인 계약 채팅방 전부)
    public Map<String, Object> login(LoginDTO dto) {
    	String email = dto.getEmail();
    	String role = dto.getRole().toLowerCase();
    	
        // role 분기 후 이메일로 조회
    	// 조회 결과 null 이면 throw new CustomException(ErrorCode.INVALID_CREDENTIALS)
    	String encodedPassword;
    	if("user".equals(role)) {
    		UserDTO user = userMapper.findByEmail(email);
    		if (user == null) throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    		encodedPassword = user.getPassword();
    		
    		// 탈퇴한 회원인지 확인
    		if (user.isDeleted()) {
    		    throw new CustomException(ErrorCode.WITHDRAWN_USER);
    		}
    	}else if("company".equals(role)) {
    		CompanyDTO company = companyMapper.findByEmail(email);
    		if (company == null) throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    		
    		//탈퇴한 회사인지 확인
			if (company.isDeleted()) {
			    throw new CustomException(ErrorCode.WITHDRAWN_COMPANY);
			}
    		encodedPassword =company.getPassword();
    	   		
    	}else {
    		throw new CustomException(ErrorCode.INVALID_ROLE);
    	}
    	
    	
    	
    	//비밀번호 검증
    	if(!passwordEncoder.matches(dto.getPassword(), encodedPassword)) {// matches(평문비번, 암호화된 비번)
    		throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    	}
    	
        //비밀번호 검증 되면 jwt 발급
    	String token = jwtTokenProvider.createAccessToken(email, role);
        
    	//채팅방 목록 조회
    	List<Integer> chatRoomIds= new ArrayList<>();
    	if("user".equals(role)) {
    		chatRoomIds= chatRoomMapper.findChatRoomIdsByFreelancerEmail(email);
    	}else if("company".equals(role)) {
    		chatRoomIds= chatRoomMapper.findChatRoomIdsByCompanyEmail(email);
    	}
    	
    	
    	//반환
    	Map<String, Object> result=new HashMap<>();
    	result.put("accessToken", token);
    	result.put("email", email);
    	result.put("role", role);
    	result.put("chatRoomIds", chatRoomIds);
        return result;
    }

    
    
    // 비밀번호 업데이트 : 이메일인증먼저 확인 + 이메일 존재 확인 → 새 비밀번호 암호화 → role 분기 UPDATE
    @Transactional
    public void resetPassword(String email, String newPassword, String role) {
    	//이메일 인증 유효한지 먼저 확인
    	String key = MailService.getVerifiedKey("pwreset",email);
    	String verified = redisTemplate.opsForValue().get(key);
        if (verified == null) throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
    	
        if("user".equals(role)) {
        	boolean existUser = userMapper.existsByEmail(email);
        	if(!existUser) throw new CustomException(ErrorCode.NOT_FOUND);
        }else if("company".equals(role)) {
        	boolean existCompany = companyMapper.existsByEmail(email);
        	if(!existCompany) throw new CustomException(ErrorCode.NOT_FOUND);
        }
       
        // 새로운 비밀번호 암호화해서 db 저정
        String encoded = passwordEncoder.encode(newPassword);
        if("user".equals(role)) {
        	userMapper.updatePassword(email, encoded);
        }else if("company".equals(role)) {
        	companyMapper.updatePassword(email, encoded);
        }
        // 비밀번호 변경 완료 후 인증 키 삭제
        redisTemplate.delete(MailService.getVerifiedKey("pwreset", email));
    }

    
    
    // 사업자 번호 유효성 검사
    public boolean verifyBusinessNumber(String businessNumber) {
    	try {
            return businessNumberValidator.validate(businessNumber);
        } catch (CustomException e) {
            throw e; // CustomException 은 그대로 전파
        } catch (Exception e) {
            throw new CustomException(ErrorCode.BUSINESS_API_ERROR); // 그 외 예외는 변환
        }
    }
    
}