package com.retail.convenience.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("transfer_order")
public class TransferOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String transferNo;

    private String fromType;

    private Long fromId;

    private String toType;

    private Long toId;

    private String status;

    private Integer totalQty;

    private String remark;

    private String appliedBy;

    private String approvedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}