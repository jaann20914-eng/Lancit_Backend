package com.ssafy.lancit.common.util;

import java.util.Locale;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;

public final class RoleUtil {

    public static final String USER = "user";
    public static final String COMPANY = "company";

    private RoleUtil() {
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            throw new CustomException(ErrorCode.INVALID_ROLE);
        }

        String normalizedRole = role.trim().toLowerCase(Locale.ROOT);
        if (!USER.equals(normalizedRole) && !COMPANY.equals(normalizedRole)) {
            throw new CustomException(ErrorCode.INVALID_ROLE);
        }
        return normalizedRole;
    }

    public static boolean isUser(String role) {
        return USER.equals(normalizeRole(role));
    }

    public static boolean isCompany(String role) {
        return COMPANY.equals(normalizeRole(role));
    }
}
