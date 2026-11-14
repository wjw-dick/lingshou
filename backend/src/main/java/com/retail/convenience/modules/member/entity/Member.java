package com.retail.convenience.modules.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("member")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String openid;

    private String unionid;

    private String phone;

    private String phoneMask;

    private String nickname;

    private String avatar;

    private Integer gender;

    private LocalDate birthday;

    private Long levelId;

    private Integer points;

    private BigDecimal walletBalance;

    private BigDecimal totalConsume;

    private Integer orderCount;

    private String tags;

    private String status;

    private LocalDateTime registeredAt;

    private LocalDateTime lastActiveAt;

}