package com.ssafy.lancit.global.enums;

import com.ssafy.lancit.common.exception.CustomException;
import com.ssafy.lancit.common.exception.ErrorCode;

public enum PortfolioCategory {
    WEB_APP,
    DESIGN,
    BRANDING,
    MARKETING,
    PLANNING;

    public static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_PORTFOLIO_CATEGORY);
        }

        try {
            return PortfolioCategory.valueOf(value.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_PORTFOLIO_CATEGORY);
        }
    }
}
