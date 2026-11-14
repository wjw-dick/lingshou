package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_publish")
public class MpPublish {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long accountId;

    private Long draftId;

    private String title;

    private String status;

    private String failReason;

    private String msgId;

    private LocalDateTime publishAt;

    private Integer readCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}