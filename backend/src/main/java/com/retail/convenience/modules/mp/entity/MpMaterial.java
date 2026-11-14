package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_material")
public class MpMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long accountId;

    private String mediaType;

    private String name;

    private String url;

    private String thumbUrl;

    private String mediaId;

    private Integer permanent;

    private Long size;

    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}