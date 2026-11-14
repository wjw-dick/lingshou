package com.retail.convenience.modules.supplier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("supplier_product")
public class SupplierProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long supplierId;

    private Long skuId;

    private BigDecimal supplyPrice;

    private String auditStatus;

    private String status;

}