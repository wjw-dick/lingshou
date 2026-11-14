package com.retail.convenience.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("stock_check_item")
public class StockCheckItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long checkId;

    private Long skuId;

    private Integer bookQty;

    private Integer actualQty;

    private Integer diffQty;

    private String reason;

}