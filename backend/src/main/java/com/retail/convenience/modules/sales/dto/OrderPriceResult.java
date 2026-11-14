package com.retail.convenience.modules.sales.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderPriceResult {

    private BigDecimal grossAmount = BigDecimal.ZERO;
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    private BigDecimal pointsDiscount = BigDecimal.ZERO;
    private BigDecimal freightAmount = BigDecimal.ZERO;
    private BigDecimal payableAmount = BigDecimal.ZERO;
    private Integer pointsUsed = 0;
    private Long memberCouponId;
    private List<Line> lines;

    @Data
    public static class Line {
        private Long skuId;
        private String skuName;
        private String barcode;
        private BigDecimal price;
        private Integer qty;
        private BigDecimal amount;
    }
}
