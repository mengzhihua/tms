package com.tms.billing.service;

import java.math.BigDecimal;

/** 把距离、车型和车牌写进运费说明，费率过程接在后面。 */
public final class QuoteNote {
    private QuoteNote() {}

    public static String explain(
            BigDecimal distanceKm, String vehicleType, String plateNo, String rateDetail) {
        StringBuilder text = new StringBuilder();
        if (distanceKm != null) {
            text.append("距离 ").append(distanceKm.stripTrailingZeros().toPlainString()).append(" 公里");
        }
        if (vehicleType != null && !vehicleType.trim().isEmpty()) {
            append(text, "车型 " + vehicleType.trim());
        }
        if (plateNo != null && !plateNo.trim().isEmpty()) {
            append(text, "车牌 " + plateNo.trim());
        }
        if (rateDetail != null && !rateDetail.trim().isEmpty()) {
            if (text.length() > 0) {
                text.append("。");
            }
            text.append(rateDetail.trim());
        }
        return text.toString();
    }

    private static void append(StringBuilder text, String piece) {
        if (text.length() > 0) {
            text.append("，");
        }
        text.append(piece);
    }
}
