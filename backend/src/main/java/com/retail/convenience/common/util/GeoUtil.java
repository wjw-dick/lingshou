package com.retail.convenience.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class GeoUtil {

    private static final double EARTH_RADIUS = 6371000d;

    private GeoUtil() {
    }

    public static double distanceMeters(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        if (lng1 == null || lat1 == null || lng2 == null || lat2 == null) {
            return Double.MAX_VALUE;
        }
        double radLat1 = Math.toRadians(lat1.doubleValue());
        double radLat2 = Math.toRadians(lat2.doubleValue());
        double deltaLat = radLat1 - radLat2;
        double deltaLng = Math.toRadians(lng1.doubleValue() - lng2.doubleValue());
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(deltaLng / 2), 2)));
        return BigDecimal.valueOf(s * EARTH_RADIUS).setScale(0, RoundingMode.HALF_UP).doubleValue();
    }
}
