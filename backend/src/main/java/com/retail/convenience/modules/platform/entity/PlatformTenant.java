package com.retail.convenience.modules.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("platform_tenant")
public class PlatformTenant {

    @TableId(type = IdType.AUTO)
    private Long tenantId;

    private String tenantCode;

    private String name;

    private String industry;

    private String contact;

    private String phone;

    private Long planId;

    private String status;

    private LocalDateTime trialEndAt;

    private LocalDateTime expireAt;

    private Integer seatLimit;

    private Integer storeLimit;

    private Integer mpAccountLimit;

    private Integer storageQuotaGb;

    private Integer smsQuota;

    private Integer apiQuota;

    private String overdueStrategy;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}