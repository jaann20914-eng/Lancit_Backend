package com.ssafy.lancit.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Locale;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;
 


// SecurityContext 에서 현재 로그인한 유저 이메일/역할 꺼내는 유틸
public class SecurityUtil {
	 
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String ROLE_USER = "USER";
    private static final String ROLE_COMPANY = "COMPANY";

    private SecurityUtil() {}
 
    // 현재 로그인한 유저(또는 회사)의 이메일 반환 :  로그인 안 된 상태면 UNAUTHORIZED 예외
    public static String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return (String) auth.getPrincipal();
    }
 
    // 현재 로그인한 역할 반환 (USER 인지 COMPANY) : 확인 불가 상태면 UNAUTHORIZED 예외
    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        String authority = auth.getAuthorities().iterator().next().getAuthority();
        return normalizeRole(authority);
    }

    private static String normalizeRole(String authority) {
        if (authority == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        String role = authority.trim();
        if (role.toUpperCase(Locale.ROOT).startsWith(ROLE_PREFIX)) {
            role = role.substring(ROLE_PREFIX.length());
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_USER.equals(normalizedRole) && !ROLE_COMPANY.equals(normalizedRole)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return normalizedRole;
    }
}
