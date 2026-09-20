package com.tms.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 与 IR 沙盘相对运价对齐：SF 2.2 / JD 1.8 / SELF01 1.4。
 * 换商时按费率比折算运费，避免 IR HTTP 下次同步把快照刷回旧金额。
 */
public final class CarrierRates {
    public static final String SF = "SF";
    public static final String JD = "JD";
    public static final String SELF01 = "SELF01";

    private CarrierRates() {
    }

    public static BigDecimal rate(String code) {
        String carrier = normalize(code);
        if (SF.equals(carrier)) {
            return BigDecimal.valueOf(2.2);
        }
        if (JD.equals(carrier)) {
            return BigDecimal.valueOf(1.8);
        }
        return BigDecimal.valueOf(1.4);
    }

    public static BigDecimal scaledFreight(
            String fromCarrier, String toCarrier, BigDecimal freight) {
        BigDecimal from = rate(fromCarrier);
        if (from.signum() <= 0 || freight == null || freight.signum() <= 0) {
            return freight;
        }
        return freight.multiply(rate(toCarrier)).divide(from, 2, RoundingMode.HALF_UP);
    }

    private static String normalize(String code) {
        if (code == null || code.trim().isEmpty()) {
            return SELF01;
        }
        String value = code.trim().toUpperCase();
        if ("SF".equals(value) || "SFEXPRESS".equals(value) || value.contains("顺丰")) {
            return SF;
        }
        if ("JD".equals(value) || "JDL".equals(value) || "JINGDONG".equals(value)
                || value.contains("京东")) {
            return JD;
        }
        return SELF01;
    }
}
