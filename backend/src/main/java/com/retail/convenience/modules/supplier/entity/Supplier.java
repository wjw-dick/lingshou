package com.retail.convenience.modules.supplier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("supplier")
public class Supplier {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private String contact;

    private String phone;

    private String address;

    private String category;

    private Integer settleCycleDays;

    private BigDecimal creditLimit;

    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}