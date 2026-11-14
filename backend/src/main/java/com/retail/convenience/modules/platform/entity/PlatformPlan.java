package com.retail.convenience.modules.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("platform_plan")
public class PlatformPlan {

    @TableId(type = IdType.AUTO)
    private Long planId;

    private String planCode;

    private String name;

    private BigDecimal price;

    private String billingCycle;

    private Integer seatLimit;

    private Integer storeLimit;

    private Integer mpAccountLimit;

    private Integer storageQuotaGb;

    private Integer smsQuota;

    private Integer apiQuota;

    private Integer trialDays;

    private String features;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}