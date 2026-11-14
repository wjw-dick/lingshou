package com.retail.convenience.modules.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("brand")
public class Brand {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String nameEn;

    private String logo;

    private Integer sortNo;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}