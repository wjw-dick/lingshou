package com.retail.convenience.modules.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("supply_order_item")
public class SupplyOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long supplyId;

    private Long skuId;

    private Integer qty;

    private Integer shippedQty;

    private Integer receivedQty;

    private BigDecimal price;

    private BigDecimal amount;

}