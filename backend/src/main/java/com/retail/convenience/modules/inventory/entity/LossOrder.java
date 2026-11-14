package com.retail.convenience.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("loss_order")
public class LossOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String lossNo;

    private Long storeId;

    private String reasonType;

    private String status;

    private Integer totalQty;

    private BigDecimal totalAmount;

    private String photos;

    private String remark;

    private String createdBy;

    private String approvedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}