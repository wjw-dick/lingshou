package com.retail.convenience.modules.sales.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PosOrderRequest {

    private Long storeId;
    private Long memberId;
    private Long memberCouponId;
    private Integer pointsUsed;
    private Boolean useWallet;
    private String paymentMethod = "CASH";
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long skuId;
        private Integer qty;
        private BigDecimal price;
    }
}
