package com.retail.convenience.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("transfer_order_item")
public class TransferOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long transferId;

    private Long skuId;

    private Integer qty;

    private String batchNo;

}