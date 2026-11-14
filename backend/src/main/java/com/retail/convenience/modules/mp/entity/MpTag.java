package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_tag")
public class MpTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long accountId;

    private Long wxTagId;

    private String name;

    private Integer fansCount;

    private String syncStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}