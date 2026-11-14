package com.retail.convenience.modules.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("store_sku_price")
public class StoreSkuPrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long skuId;

    private BigDecimal priceRetail;

    private BigDecimal priceMember;

}