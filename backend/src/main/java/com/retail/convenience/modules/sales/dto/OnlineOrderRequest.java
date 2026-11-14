package com.retail.convenience.modules.sales.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OnlineOrderRequest {

    private Long storeId;
    private String deliveryType = "PICKUP";
    private Long memberCouponId;
    private Integer pointsUsed = 0;
    private String deliveryName;
    private String deliveryPhone;
    private String deliveryAddress;
    private BigDecimal deliveryLng;
    private BigDecimal deliveryLat;
    private String expectedTime;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long skuId;
        private Integer qty;
    }
}
