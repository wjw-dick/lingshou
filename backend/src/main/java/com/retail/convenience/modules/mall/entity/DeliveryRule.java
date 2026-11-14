package com.retail.convenience.modules.mall.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("delivery_rule")
public class DeliveryRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private BigDecimal minOrderAmount;

    private BigDecimal deliveryFee;

    private BigDecimal freeThreshold;

    private Integer radiusM;

    private String timeSlots;

    private Integer enabled;

}