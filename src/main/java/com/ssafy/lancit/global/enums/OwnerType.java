package com.ssafy.lancit.global.enums;

import com.ssafy.lancit.common.util.RoleUtil;

public enum OwnerType {
    user, company;

    public static OwnerType fromRole(String role) {
        return OwnerType.valueOf(RoleUtil.normalizeRole(role));
    }
}
