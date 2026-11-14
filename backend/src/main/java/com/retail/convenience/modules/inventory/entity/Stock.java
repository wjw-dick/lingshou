package com.retail.convenience.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("stock")
public class Stock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ownerType;

    private Long ownerId;

    private Long skuId;

    private Integer qty;

    private Integer lockedQty;

    private Integer safetyStock;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}