package com.retail.convenience.modules.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private String type;

    private String content;

    private String images;

    private String status;

    private String reply;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}