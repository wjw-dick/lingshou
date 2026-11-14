package com.retail.convenience.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("stock_flow")
public class StockFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ownerType;

    private Long ownerId;

    private Long skuId;

    private String bizType;

    private Integer changeQty;

    private Integer beforeQty;

    private Integer afterQty;

    private String refType;

    private Long refId;

    private String remark;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}