package com.retail.convenience.modules.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("promotion_item")
public class PromotionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long promotionId;

    private Long skuId;

    private BigDecimal originalPrice;

    private BigDecimal promoPrice;

    private BigDecimal discount;

}