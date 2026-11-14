package com.retail.convenience.modules.supplier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("supplier_qualification")
public class SupplierQualification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long supplierId;

    private String type;

    private String fileUrl;

    private LocalDate expireAt;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}