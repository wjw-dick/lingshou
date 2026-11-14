package com.retail.convenience.modules.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_spu")
public class ProductSpu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String spuCode;

    private String name;

    private String nameEn;

    private Long categoryId;

    private Long brandId;

    private String unit;

    private String spec;

    private Integer isWeight;

    private String storageCondition;

    private Integer shelfLifeDays;

    private String origin;

    private String ingredients;

    private String allergens;

    private Integer isTobacco;

    private Integer onlineSale;

    private String status;

    private String mainImage;

    private String images;

    private String detailHtml;

    private String detailHtmlEn;

    private BigDecimal priceGuide;

    private BigDecimal priceRetail;

    private BigDecimal priceMember;

    private BigDecimal priceFranchise;

    private String tags;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}