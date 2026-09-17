package com.tms.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CarrierRatesTest {
    @Test
    void scaledFreightFollowsRateRatio() {
        assertEquals(0, new BigDecimal("70.00").compareTo(
                CarrierRates.scaledFreight("SF", "SELF01", new BigDecimal("110"))));
        assertEquals(0, new BigDecimal("110").compareTo(
                CarrierRates.scaledFreight("SF", "SF", new BigDecimal("110"))));
        assertEquals(0, new BigDecimal("11.45").compareTo(
                CarrierRates.scaledFreight("SF", "SELF01", new BigDecimal("18"))));
    }
}
