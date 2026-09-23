package com.tms.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 在原费率之外，按距离每公里 1 元、按车型加固定费。 */
public final class FareFormula {
    private static final BigDecimal PER_KM = new BigDecimal("1.00");

    private FareFormula() {}

    public static BigDecimal distanceFee(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.signum() <= 0) {
            return money(BigDecimal.ZERO);
        }
        return money(distanceKm.multiply(PER_KM));
    }

    public static BigDecimal vehicleFee(String vehicleType) {
        String type = vehicleType == null ? "" : vehicleType.trim().toUpperCase();
        if (type.contains("9.6") || type.contains("9M6")) {
            return new BigDecimal("50.00");
        }
        if (type.contains("6.8") || type.contains("6M8")) {
            return new BigDecimal("35.00");
        }
        if (type.contains("4.2") || type.contains("4M2")) {
            return new BigDecimal("20.00");
        }
        return money(BigDecimal.ZERO);
    }

    public static BigDecimal total(BigDecimal base, BigDecimal distanceKm, String vehicleType) {
        BigDecimal start = base == null ? BigDecimal.ZERO : base;
        return money(start.add(distanceFee(distanceKm)).add(vehicleFee(vehicleType)));
    }

    public static String detail(String rateDetail, BigDecimal distanceKm, String vehicleType) {
        StringBuilder text = new StringBuilder();
        if (rateDetail != null && !rateDetail.trim().isEmpty()) {
            text.append(rateDetail.trim());
        }
        BigDecimal distance = distanceFee(distanceKm);
        BigDecimal vehicle = vehicleFee(vehicleType);
        if (distance.signum() == 0 && vehicle.signum() == 0) {
            return text.toString();
        }
        if (text.length() > 0) {
            text.append("。");
        }
        text.append("距离 ")
                .append(distanceKm == null ? "0" : distanceKm.stripTrailingZeros().toPlainString())
                .append(" 公里 × 1 元");
        if (vehicle.signum() > 0) {
            text.append("，车型 ")
                    .append(vehicleType.trim())
                    .append(" 加 ")
                    .append(vehicle.stripTrailingZeros().toPlainString())
                    .append(" 元");
        }
        return text.toString();
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
