package com.retail.convenience.modules.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("member_level")
public class MemberLevel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private Integer minPoints;

    private BigDecimal discount;

    private String benefits;

}