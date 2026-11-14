package com.retail.convenience.modules.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("platform_bill")
public class PlatformBill {

    @TableId(type = IdType.AUTO)
    private Long billId;

    private Long tenantId;

    private String billNo;

    private String period;

    private String item;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private String status;

    private LocalDate dueDate;

    private LocalDateTime paidAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}