package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_account")
public class MpAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String name;

    private String appId;

    private String appSecret;

    private String serverUrl;

    private String token;

    private String aesKey;

    private String oauthDomain;

    private String jsDomain;

    private String qrUrl;

    private Integer apiQuotaUsed;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}