package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_message")
public class MpMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long accountId;

    private String openid;

    private String msgType;

    private String direction;

    private String content;

    private String rawPayload;

    private Integer errCode;

    private String errMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}