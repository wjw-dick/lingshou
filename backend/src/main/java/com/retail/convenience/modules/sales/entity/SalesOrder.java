package com.retail.convenience.modules.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sales_order")
public class SalesOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String orderNo;

    private String channel;

    private Long storeId;

    private Long memberId;

    private String deliveryType;

    private String status;

    private String payStatus;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal freightAmount;

    private BigDecimal payableAmount;

    private BigDecimal paidAmount;

    private Long memberCouponId;

    private Integer pointsUsed;

    private BigDecimal pointsDiscount;

    private BigDecimal walletPaid;

    private String paymentMethod;

    private String deliveryName;

    private String deliveryPhone;

    private String deliveryAddress;

    private String expectedTime;

    private LocalDateTime acceptDeadline;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    private LocalDateTime completedAt;

}