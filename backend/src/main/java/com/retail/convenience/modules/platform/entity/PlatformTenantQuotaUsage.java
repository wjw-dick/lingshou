package com.retail.convenience.modules.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("platform_tenant_quota_usage")
public class PlatformTenantQuotaUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Integer seatUsed;

    private Integer storeUsed;

    private Integer mpUsed;

    private BigDecimal storageUsedGb;

    private Integer smsUsed;

    private Long apiUsed;

    private LocalDate statDate;

}