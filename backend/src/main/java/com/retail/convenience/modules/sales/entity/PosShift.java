package com.retail.convenience.modules.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pos_shift")
public class PosShift {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String shiftNo;

    private Long storeId;

    private Long cashierId;

    private String cashierName;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private BigDecimal openingCash;

    private BigDecimal cashSales;

    private BigDecimal wechatSales;

    private BigDecimal alipaySales;

    private BigDecimal walletSales;

    private BigDecimal expectedCash;

    private BigDecimal actualCash;

    private BigDecimal diffAmount;

    private String diffReason;

    private String status;

}