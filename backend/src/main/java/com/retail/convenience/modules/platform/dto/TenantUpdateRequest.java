package com.retail.convenience.modules.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantUpdateRequest {

    private String name;
    private String industry;
    private String contact;
    private String phone;
    private Long planId;
    private Integer seatLimit;
    private Integer storeLimit;
    private Integer mpAccountLimit;
    private Integer storageQuotaGb;
    private Integer smsQuota;
    private Integer apiQuota;
    private LocalDateTime expireAt;
    private String remark;
}
