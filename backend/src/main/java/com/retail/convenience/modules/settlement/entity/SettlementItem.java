package com.retail.convenience.modules.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("settlement_item")
public class SettlementItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long statementId;

    private String refType;

    private Long refId;

    private String refNo;

    private BigDecimal amount;

    private BigDecimal deduction;

    private BigDecimal payable;

}