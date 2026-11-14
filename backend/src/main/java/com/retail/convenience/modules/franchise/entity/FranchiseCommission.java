package com.retail.convenience.modules.franchise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("franchise_commission")
public class FranchiseCommission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long franchiseeId;

    private Long storeId;

    private String period;

    private BigDecimal salesAmount;

    private String commissionType;

    private BigDecimal commissionValue;

    private BigDecimal commissionAmount;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}