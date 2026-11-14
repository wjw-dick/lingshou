package com.retail.convenience.modules.mp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("mp_stat")
public class MpStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long accountId;

    private LocalDate statDate;

    private Integer newUsers;

    private Integer totalUsers;

    private Integer msgCount;

    private Integer apiCount;

}