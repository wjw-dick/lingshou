package com.retail.convenience.modules.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@TableName("store")
public class Store {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String code;

    private String name;

    private String type;

    private String region;

    private String address;

    private BigDecimal lng;

    private BigDecimal lat;

    private String businessHours;

    private String phone;

    private String managerName;

    private String managerPhone;

    private Integer deliveryRadiusM;

    private Integer deliveryEnabled;

    private Integer onlineEnabled;

    private String status;

    private LocalDate openedAt;

    private LocalDate licenseExpireAt;

    private String images;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}