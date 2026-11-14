package com.retail.convenience.modules.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_order")
public class RefundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String refundNo;

    private Long orderId;

    private Long storeId;

    private String type;

    private BigDecimal amount;

    private String reason;

    private String status;

    private String items;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}