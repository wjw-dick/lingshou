package com.retail.convenience.modules.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement_invoice")
public class SettlementInvoice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long statementId;

    private String invoiceNo;

    private BigDecimal amount;

    private String fileUrl;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}