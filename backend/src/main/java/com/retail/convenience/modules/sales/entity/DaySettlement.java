package com.retail.convenience.modules.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("day_settlement")
public class DaySettlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private LocalDate settleDate;

    private Integer orderCount;

    private BigDecimal salesAmount;

    private BigDecimal refundAmount;

    private BigDecimal netAmount;

    private BigDecimal cashAmount;

    private BigDecimal wechatAmount;

    private BigDecimal alipayAmount;

    private BigDecimal walletAmount;

    private BigDecimal diffAmount;

    private String diffReason;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}