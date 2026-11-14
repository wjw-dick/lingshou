package com.retail.convenience.common.security;

import lombok.Data;

@Data
public class LoginUser {

    private Long userId;
    private String username;
    private String realName;
    private String userType;
    private String roleCode;
    private Long tenantId;
    private String tenantCode;
    private Long storeId;
    private Long supplierId;
    private Long franchiseeId;
    private Long memberId;
    private String tokenId;

    public boolean isPlatform() {
        return "PLATFORM".equals(userType);
    }

    public boolean isTenant() {
        return "TENANT".equals(userType);
    }

    public boolean isMember() {
        return "MEMBER".equals(userType);
    }
}
