package com.retail.convenience.modules.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon")
public class Coupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String type;

    private BigDecimal thresholdAmount;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private Long skuId;

    private Integer totalQty;

    private Integer issuedQty;

    private Integer perUserLimit;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}