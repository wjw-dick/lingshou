package com.retail.convenience.modules.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("product_barcode")
public class ProductBarcode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long skuId;

    private String barcode;

}