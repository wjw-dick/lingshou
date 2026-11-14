package com.retail.convenience.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("stock_check")
public class StockCheck {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String checkNo;

    private Long storeId;

    private String checkType;

    private String status;

    private Integer totalDiffQty;

    private String remark;

    private String createdBy;

    private String approvedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}