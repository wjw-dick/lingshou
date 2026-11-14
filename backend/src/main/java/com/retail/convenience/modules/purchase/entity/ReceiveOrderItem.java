package com.retail.convenience.modules.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("receive_order_item")
public class ReceiveOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long receiveId;

    private Long supplyItemId;

    private Long skuId;

    private Integer qty;

    private Integer actualQty;

    private Integer diffQty;

    private LocalDate productionDate;

    private LocalDate expiryDate;

    private BigDecimal coldChainTemp;

    private String batchNo;

}