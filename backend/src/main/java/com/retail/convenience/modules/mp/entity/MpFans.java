package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_fans")
public class MpFans {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long accountId;

    private String openid;

    private String unionid;

    private String nickname;

    private String avatar;

    private Integer gender;

    private String region;

    private String remark;

    private Integer subscribeStatus;

    private LocalDateTime subscribeTime;

    private String tags;

    private LocalDateTime syncedAt;

}