package com.retail.convenience.modules.member.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("member_favorite")
public class MemberFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long memberId;

    private Long skuId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}