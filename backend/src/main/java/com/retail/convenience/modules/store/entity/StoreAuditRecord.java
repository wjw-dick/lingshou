package com.retail.convenience.modules.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("store_audit_record")
public class StoreAuditRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private String supervisor;

    private LocalDate checkDate;

    private Integer score;

    private String issues;

    private String photos;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}