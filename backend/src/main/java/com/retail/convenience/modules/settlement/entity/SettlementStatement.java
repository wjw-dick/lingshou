package com.retail.convenience.modules.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("settlement_statement")
public class SettlementStatement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String statementNo;

    private String type;

    private Long partnerId;

    private String partnerName;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private BigDecimal totalAmount;

    private BigDecimal deductionAmount;

    private BigDecimal payableAmount;

    private String status;

    private LocalDateTime confirmedAt;

    private LocalDateTime paidAt;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}