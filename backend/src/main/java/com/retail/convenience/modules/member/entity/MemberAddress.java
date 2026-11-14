package com.retail.convenience.modules.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("member_address")
public class MemberAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private String name;

    private String phone;

    private String address;

    private BigDecimal lng;

    private BigDecimal lat;

    private Integer isDefault;

}