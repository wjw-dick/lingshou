package com.retail.convenience.modules.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {

    private String token;
    private UserInfo user;
    private List<MenuNode> menus;

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String realName;
        private String userType;
        private String roleCode;
        private Long tenantId;
        private String tenantCode;
        private String tenantName;
        private Long storeId;
        private Long supplierId;
        private Long franchiseeId;
        private Long memberId;
    }

    @Data
    public static class MenuNode {
        private Long id;
        private Long parentId;
        private String title;
        private String titleEn;
        private String icon;
        private String path;
        private String component;
        private String permission;
        private Integer sortNo;
        private List<MenuNode> children;
    }
}
