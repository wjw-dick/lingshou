package com.retail.convenience.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("loss_order_item")
public class LossOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long lossId;

    private Long skuId;

    private String batchNo;

    private Integer qty;

    private BigDecimal amount;

}