package com.retail.convenience.modules.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long purchaseId;

    private Long skuId;

    private Integer qty;

    private Integer receivedQty;

    private BigDecimal price;

    private BigDecimal amount;

}