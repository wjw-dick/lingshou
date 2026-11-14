package com.retail.convenience.modules.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("difference_order")
public class DifferenceOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String diffNo;

    private Long receiveId;

    private Long supplyId;

    private Long storeId;

    private String type;

    private String content;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}