package com.retail.convenience.modules.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_sku")
public class ProductSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long spuId;

    private String skuCode;

    private String barcode;

    private String spec;

    private String unit;

    private Integer isWeight;

    private BigDecimal costPrice;

    private BigDecimal priceGuide;

    private BigDecimal priceRetail;

    private BigDecimal priceMember;

    private BigDecimal priceFranchise;

    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}