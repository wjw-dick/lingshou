package com.retail.convenience.modules.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("sales_order_item")
public class SalesOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long orderId;

    private Long skuId;

    private String skuName;

    private String barcode;

    private BigDecimal price;

    private Integer qty;

    private BigDecimal amount;

}