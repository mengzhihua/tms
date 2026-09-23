package com.tms.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FareFormulaTest {
    @Test
    void distanceAndVehicleAddOntoTheBaseFare() {
        BigDecimal total = FareFormula.total(new BigDecimal("12"), new BigDecimal("10"), "TRUCK_4M2");
        assertEquals(0, total.compareTo(new BigDecimal("42.00")));
        assertEquals(
                "首单位 1 12元。距离 10 公里 × 1 元，车型 TRUCK_4M2 加 20 元",
                FareFormula.detail("首单位 1 12元", new BigDecimal("10"), "TRUCK_4M2"));
    }

    @Test
    void unknownVehicleAndZeroDistanceKeepTheBaseFare() {
        assertEquals(0, FareFormula.total(new BigDecimal("12.50"), BigDecimal.ZERO, "面包车").compareTo(new BigDecimal("12.50")));
        assertEquals(0, FareFormula.vehicleFee("6.8米").compareTo(new BigDecimal("35.00")));
        assertEquals(0, FareFormula.vehicleFee("9.6米").compareTo(new BigDecimal("50.00")));
    }
}
