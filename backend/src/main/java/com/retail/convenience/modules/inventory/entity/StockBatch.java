package com.retail.convenience.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("stock_batch")
public class StockBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ownerType;

    private Long ownerId;

    private Long skuId;

    private String batchNo;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    private Integer qty;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}