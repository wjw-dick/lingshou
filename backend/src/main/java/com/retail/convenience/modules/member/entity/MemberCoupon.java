package com.retail.convenience.modules.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("member_coupon")
public class MemberCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long couponId;

    private Long memberId;

    private String status;

    private LocalDateTime receivedAt;

    private LocalDateTime usedAt;

    private Long orderId;

}