package com.retail.convenience.modules.franchise.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("franchisee")
public class Franchisee {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private String contact;

    private String phone;

    private String level;

    private String commissionType;

    private BigDecimal commissionValue;

    private BigDecimal creditLimit;

    private BigDecimal usedCredit;

    private BigDecimal deposit;

    private String contractNo;

    private LocalDate contractStart;

    private LocalDate contractEnd;

    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}