package com.ssafy.lancit.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
import com.ssafy.lancit.common.jwt.JwtTokenProvider;
import com.ssafy.lancit.common.util.BusinessNumberValidator;
import com.ssafy.lancit.domain.auth.dto.LoginDTO;
import com.ssafy.lancit.domain.auth.dto.SignupDTO;
import com.ssafy.lancit.domain.auth.service.AuthService;
import com.ssafy.lancit.domain.company.dto.CompanyDTO;
import com.ssafy.lancit.domain.company.mapper.CompanyMapper;
import com.ssafy.lancit.domain.contract.mapper.ChatRoomMapper;
import com.ssafy.lancit.domain.user.dto.UserDTO;
import com.ssafy.lancit.domain.user.mapper.UserMapper;
import com.ssafy.lancit.global.enums.JobCategory;

@MockitoSettings(strictness = Strictness.LENIENT) // 추가
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserMapper userMapper;
    @Mock private CompanyMapper companyMapper;
    @Mock private ChatRoomMapper chatRoomMapper;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private BusinessNumberValidator businessNumberValidator;
    @Mock private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    // ═══════════════════════════════════════════════════════
    //  signup()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("signup() 회원가입")
    class SignupTest {

        private SignupDTO userDto;
        private SignupDTO companyDto;

        @BeforeEach
        void setUp() {
            userDto = new SignupDTO();
            userDto.setEmail("user@test.com");
            userDto.setPassword("password123");
            userDto.setName("홍길동");
            userDto.setPhone("010-1234-5678");
            userDto.setJobCategory(JobCategory.IT);
            userDto.setRole("user");

            companyDto = new SignupDTO();
            companyDto.setEmail("company@test.com");
            companyDto.setPassword("password123");
            companyDto.setName("담당자");
            companyDto.setCompanyName("테스트회사");
            companyDto.setPhone("010-9999-0000");
            companyDto.setJobCategory(JobCategory.IT);
            companyDto.setRole("company");
        }

        @Test
        @DisplayName("이메일 인증 미완료 시 예외")
        void signup_emailNotVerified() {
            given(valueOps.get("signup:verified:user@test.com")).willReturn(null);

            assertThatThrownBy(() -> authService.signup(userDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.EMAIL_NOT_VERIFIED.getMessage());
        }

        @Test
        @DisplayName("잘못된 role 값 시 예외")
        void signup_invalidRole() {
            given(valueOps.get("signup:verified:user@test.com")).willReturn("true");
            userDto.setRole("admin");

            assertThatThrownBy(() -> authService.signup(userDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.INVALID_ROLE.getMessage());
        }

        @Test
        @DisplayName("company 테이블에 이메일 중복 시 예외")
        void signup_duplicateEmail_company() {
            given(valueOps.get("signup:verified:user@test.com")).willReturn("true");
            given(companyMapper.existsByEmail("user@test.com")).willReturn(true);

            assertThatThrownBy(() -> authService.signup(userDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.DUPLICATE_EMAIL.getMessage());
        }

        @Test
        @DisplayName("user 테이블에 이메일 중복 시 예외")
        void signup_duplicateEmail_user() {
            given(valueOps.get("signup:verified:user@test.com")).willReturn("true");
            given(companyMapper.existsByEmail("user@test.com")).willReturn(false);
            given(userMapper.existsByEmail("user@test.com")).willReturn(true);

            assertThatThrownBy(() -> authService.signup(userDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.DUPLICATE_EMAIL.getMessage());
        }

        @Test
        @DisplayName("정상 user 회원가입 성공")
        void signup_user_success() {
            given(valueOps.get("signup:verified:user@test.com")).willReturn("true");
            given(companyMapper.existsByEmail(anyString())).willReturn(false);
            given(userMapper.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPw");

            authService.signup(userDto);

            verify(userMapper, times(1)).insert(any(UserDTO.class));
            verify(companyMapper, never()).insert(any());
            verify(redisTemplate, times(1)).delete("signup:verified:user@test.com");
        }

        @Test
        @DisplayName("정상 company 회원가입 성공 (사업자번호 없음)")
        void signup_company_success_noBusiness() {
            given(valueOps.get("signup:verified:company@test.com")).willReturn("true");
            given(companyMapper.existsByEmail(anyString())).willReturn(false);
            given(userMapper.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPw");

            authService.signup(companyDto);

            verify(companyMapper, times(1)).insert(any(CompanyDTO.class));
            verify(userMapper, never()).insert(any());
            verify(redisTemplate, times(1)).delete("signup:verified:company@test.com");
        }

        @Test
        @DisplayName("정상 company 회원가입 성공 (사업자번호 있음)")
        void signup_company_success_withBusiness() {
            companyDto.setBusinessNumber("1234567890");
            given(valueOps.get("signup:verified:company@test.com")).willReturn("true");
            given(companyMapper.existsByEmail(anyString())).willReturn(false);
            given(userMapper.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPw");
            given(businessNumberValidator.validate("1234567890")).willReturn(true);

            authService.signup(companyDto);

            verify(companyMapper, times(1)).insert(argThat(dto ->
                    dto.isBusinessNumberVerified() == true
            ));
        }

        @Test
        @DisplayName("사업자번호 검증 실패 시 예외")
        void signup_company_invalidBusiness() {
            companyDto.setBusinessNumber("0000000000");
            given(valueOps.get("signup:verified:company@test.com")).willReturn("true");
            given(companyMapper.existsByEmail(anyString())).willReturn(false);
            given(userMapper.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPw");
            given(businessNumberValidator.validate("0000000000")).willReturn(false);

            assertThatThrownBy(() -> authService.signup(companyDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.BUSINESS_API_ERROR.getMessage());
        }

        @Test
        @DisplayName("DB INSERT 실패 시 Redis 인증키 삭제 안 됨 (트랜잭션 롤백)")
        void signup_dbFail_redisNotDeleted() {
            given(valueOps.get("signup:verified:user@test.com")).willReturn("true");
            given(companyMapper.existsByEmail(anyString())).willReturn(false);
            given(userMapper.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPw");
            doThrow(new RuntimeException("DB 오류")).when(userMapper).insert(any());

            assertThatThrownBy(() -> authService.signup(userDto))
                    .isInstanceOf(RuntimeException.class);

            verify(redisTemplate, never()).delete(anyString());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  login()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("login() 로그인")
    class LoginTest {

        private LoginDTO loginDto;

        @BeforeEach
        void setUp() {
            loginDto = new LoginDTO();
            loginDto.setEmail("user@test.com");
            loginDto.setPassword("password123");
            loginDto.setRole("user");
        }

        @Test
        @DisplayName("잘못된 role 시 예외")
        void login_invalidRole() {
            loginDto.setRole("admin");

            assertThatThrownBy(() -> authService.login(loginDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.INVALID_ROLE.getMessage());
        }

        @Test
        @DisplayName("user 이메일 없음 시 예외")
        void login_user_notFound() {
            given(userMapper.findByEmail("user@test.com")).willReturn(null);

            assertThatThrownBy(() -> authService.login(loginDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        @Test
        @DisplayName("company 이메일 없음 시 예외")
        void login_company_notFound() {
            loginDto.setRole("company");
            loginDto.setEmail("company@test.com");
            given(companyMapper.findByEmail("company@test.com")).willReturn(null);

            assertThatThrownBy(() -> authService.login(loginDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        @Test
        @DisplayName("user 비밀번호 불일치 시 예외")
        void login_user_wrongPassword() {
            UserDTO user = UserDTO.builder().email("user@test.com").password("encodedPw").build();
            given(userMapper.findByEmail("user@test.com")).willReturn(user);
            given(passwordEncoder.matches("password123", "encodedPw")).willReturn(false);

            assertThatThrownBy(() -> authService.login(loginDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        @Test
        @DisplayName("company 비밀번호 불일치 시 예외")
        void login_company_wrongPassword() {
            loginDto.setRole("company");
            loginDto.setEmail("company@test.com");
            CompanyDTO company = CompanyDTO.builder().email("company@test.com").password("encodedPw").build();
            given(companyMapper.findByEmail("company@test.com")).willReturn(company);
            given(passwordEncoder.matches("password123", "encodedPw")).willReturn(false);

            assertThatThrownBy(() -> authService.login(loginDto))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.INVALID_CREDENTIALS.getMessage());
        }

        @Test
        @DisplayName("정상 user 로그인 성공 - accessToken, chatRoomIds 포함")
        void login_user_success() {
            UserDTO user = UserDTO.builder().email("user@test.com").password("encodedPw").build();
            given(userMapper.findByEmail("user@test.com")).willReturn(user);
            given(passwordEncoder.matches("password123", "encodedPw")).willReturn(true);
            given(jwtTokenProvider.createAccessToken("user@test.com", "user")).willReturn("jwt.token");
            given(chatRoomMapper.findChatRoomIdsByEmail("user@test.com")).willReturn(List.of(1, 2, 3));

            Map<String, Object> result = authService.login(loginDto);

            assertThat(result.get("accessToken")).isEqualTo("jwt.token");
            assertThat(result.get("email")).isEqualTo("user@test.com");
            assertThat(result.get("role")).isEqualTo("user");
            assertThat(result.get("chatRoomIds")).isEqualTo(List.of(1, 2, 3));
        }

        @Test
        @DisplayName("정상 company 로그인 성공")
        void login_company_success() {
            loginDto.setRole("company");
            loginDto.setEmail("company@test.com");
            CompanyDTO company = CompanyDTO.builder().email("company@test.com").password("encodedPw").build();
            given(companyMapper.findByEmail("company@test.com")).willReturn(company);
            given(passwordEncoder.matches("password123", "encodedPw")).willReturn(true);
            given(jwtTokenProvider.createAccessToken("company@test.com", "company")).willReturn("jwt.token");
            given(chatRoomMapper.findChatRoomIdsByEmail("company@test.com")).willReturn(List.of());

            Map<String, Object> result = authService.login(loginDto);

            assertThat(result.get("accessToken")).isEqualTo("jwt.token");
            assertThat((List<?>) result.get("chatRoomIds")).isEmpty();
        }

        @Test
        @DisplayName("채팅방 없는 user 로그인 - chatRoomIds 빈 리스트")
        void login_user_noChatRooms() {
            UserDTO user = UserDTO.builder().email("user@test.com").password("encodedPw").build();
            given(userMapper.findByEmail("user@test.com")).willReturn(user);
            given(passwordEncoder.matches("password123", "encodedPw")).willReturn(true);
            given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("token");
            given(chatRoomMapper.findChatRoomIdsByEmail("user@test.com")).willReturn(List.of());

            Map<String, Object> result = authService.login(loginDto);

            assertThat((List<?>) result.get("chatRoomIds")).isEmpty();
        }

        @Test
        @DisplayName("role 대소문자 구분 없이 처리 - USER → user 변환")
        void login_roleUpperCase() {
            loginDto.setRole("USER");
            UserDTO user = UserDTO.builder().email("user@test.com").password("encodedPw").build();
            given(userMapper.findByEmail("user@test.com")).willReturn(user);
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
            given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("token");
            given(chatRoomMapper.findChatRoomIdsByEmail(anyString())).willReturn(List.of());

            Map<String, Object> result = authService.login(loginDto);

            assertThat(result.get("role")).isEqualTo("user");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  resetPassword()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("resetPassword() 비밀번호 변경")
    class ResetPasswordTest {

        @Test
        @DisplayName("이메일 인증 미완료 시 예외")
        void reset_emailNotVerified() {
            given(valueOps.get("pwreset:verified:user@test.com")).willReturn(null);

            assertThatThrownBy(() -> authService.resetPassword("user@test.com", "newPw", "user"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.EMAIL_NOT_VERIFIED.getMessage());
        }

        @Test
        @DisplayName("user 이메일 존재하지 않을 시 예외")
        void reset_user_notFound() {
            given(valueOps.get("pwreset:verified:user@test.com")).willReturn("true");
            given(userMapper.existsByEmail("user@test.com")).willReturn(false);

            assertThatThrownBy(() -> authService.resetPassword("user@test.com", "newPw", "user"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("company 이메일 존재하지 않을 시 예외")
        void reset_company_notFound() {
            given(valueOps.get("pwreset:verified:company@test.com")).willReturn("true");
            given(companyMapper.existsByEmail("company@test.com")).willReturn(false);

            assertThatThrownBy(() -> authService.resetPassword("company@test.com", "newPw", "company"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("정상 user 비밀번호 변경 성공")
        void reset_user_success() {
            given(valueOps.get("pwreset:verified:user@test.com")).willReturn("true");
            given(userMapper.existsByEmail("user@test.com")).willReturn(true);
            given(passwordEncoder.encode("newPw")).willReturn("encodedNewPw");

            authService.resetPassword("user@test.com", "newPw", "user");

            verify(userMapper, times(1)).updatePassword("user@test.com", "encodedNewPw");
            verify(redisTemplate, times(1)).delete("pwreset:verified:user@test.com");
        }

        @Test
        @DisplayName("정상 company 비밀번호 변경 성공")
        void reset_company_success() {
            given(valueOps.get("pwreset:verified:company@test.com")).willReturn("true");
            given(companyMapper.existsByEmail("company@test.com")).willReturn(true);
            given(passwordEncoder.encode("newPw")).willReturn("encodedNewPw");

            authService.resetPassword("company@test.com", "newPw", "company");

            verify(companyMapper, times(1)).updatePassword("company@test.com", "encodedNewPw");
            verify(redisTemplate, times(1)).delete("pwreset:verified:company@test.com");
        }

        @Test
        @DisplayName("비밀번호 변경 완료 후 Redis 인증키 삭제 확인")
        void reset_redisKeyDeleted() {
            given(valueOps.get("pwreset:verified:user@test.com")).willReturn("true");
            given(userMapper.existsByEmail("user@test.com")).willReturn(true);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPw");

            authService.resetPassword("user@test.com", "newPw", "user");

            verify(redisTemplate).delete("pwreset:verified:user@test.com");
        }

        @Test
        @DisplayName("인증 완료 키로 재사용 불가 - 삭제 후 재호출 시 예외")
        void reset_cannotReuse() {
            // 첫 번째 호출: 성공
            given(valueOps.get("pwreset:verified:user@test.com")).willReturn("true");
            given(userMapper.existsByEmail("user@test.com")).willReturn(true);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPw");

            authService.resetPassword("user@test.com", "newPw", "user");

            // 두 번째 호출: 키 삭제됐으므로 null 반환
            given(valueOps.get("pwreset:verified:user@test.com")).willReturn(null);

            assertThatThrownBy(() -> authService.resetPassword("user@test.com", "newPw2", "user"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.EMAIL_NOT_VERIFIED.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  verifyBusinessNumber()
    // ═══════════════════════════════════════════════════════
    @Nested
    @DisplayName("verifyBusinessNumber() 사업자번호 검증")
    class BusinessNumberTest {

        @Test
        @DisplayName("유효한 사업자번호 - true 반환")
        void verify_valid() {
            given(businessNumberValidator.validate("6888801923")).willReturn(true);

            assertThat(authService.verifyBusinessNumber("6888801923")).isTrue();
        }

        @Test
        @DisplayName("유효하지 않은 사업자번호 - false 반환")
        void verify_invalid() {
            given(businessNumberValidator.validate("0000000000")).willReturn(false);

            assertThat(authService.verifyBusinessNumber("0000000000")).isFalse();
        }

        @Test
        @DisplayName("API 호출 실패 시 예외")
        void verify_apiFailed() {
            given(businessNumberValidator.validate(anyString()))
                    .willThrow(new CustomException(ErrorCode.BUSINESS_API_ERROR));

            assertThatThrownBy(() -> authService.verifyBusinessNumber("9999999999"))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining(ErrorCode.BUSINESS_API_ERROR.getMessage());
        }
    }
}