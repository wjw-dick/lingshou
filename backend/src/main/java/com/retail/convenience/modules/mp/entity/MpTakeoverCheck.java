package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mp_takeover_check")
public class MpTakeoverCheck {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long accountId;

    private Integer serverUrlConfigured;

    private Integer fansActive;

    private String menuSnapshot;

    private String publishSnapshot;

    private String conclusion;

    private String remark;

    private LocalDateTime checkedAt;

}