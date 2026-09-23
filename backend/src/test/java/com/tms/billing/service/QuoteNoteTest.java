package com.tms.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuoteNoteTest {
    @Test
    void distanceAndVehicleLeadTheRateDetail() {
        String note =
                QuoteNote.explain(new BigDecimal("12.50"), "4.2米", "沪A12345", "首单位 1 12元");
        assertEquals("距离 12.5 公里，车型 4.2米，车牌 沪A12345。首单位 1 12元", note);
    }

    @Test
    void blankVehicleStillKeepsDistance() {
        assertEquals("距离 0 公里。续单位", QuoteNote.explain(BigDecimal.ZERO, "  ", null, "续单位"));
    }
}
