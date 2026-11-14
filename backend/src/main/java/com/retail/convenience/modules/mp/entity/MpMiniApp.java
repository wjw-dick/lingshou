package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_mini_app")
public class MpMiniApp {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String appType;

    private String name;

    private String appId;

    private String appSecret;

    private String requestDomain;

    private String uploadDomain;

    private String downloadDomain;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}