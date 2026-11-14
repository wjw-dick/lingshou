package com.retail.convenience.modules.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("supply_order")
public class SupplyOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String orderNo;

    private String sourceType;

    private Long supplierId;

    private Long storeId;

    private Long franchiseeId;

    private String status;

    private BigDecimal totalAmount;

    private LocalDate expectedAt;

    private String logisticsNo;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}