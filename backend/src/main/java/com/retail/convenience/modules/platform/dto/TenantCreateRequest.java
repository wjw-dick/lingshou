package com.retail.convenience.modules.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantCreateRequest {

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    private String name;

    private String industry = "连锁便利店";
    private String contact;
    private String phone;

    @NotNull(message = "套餐不能为空")
    private Long planId;

    @NotBlank(message = "管理员账号不能为空")
    private String adminUsername;

    private String adminPassword = "Admin@123";
    private Integer storeLimit;
}
