package com.retail.convenience.modules.auth.dto;

import lombok.Data;

@Data
public class AppLoginRequest {

    private String tenantCode;
    private String openid;
    private String unionid;
    private String nickname;
    private String phone;
}
