package com.tms.dispatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class CarrierScoreTest {
    @Test
    void perfectTripScoresOneHundred() {
        CarrierScore.Result result =
                CarrierScore.score(
                        "SF",
                        Collections.singletonList(
                                new CarrierScore.Trip(true, false, new BigDecimal("100"), new BigDecimal("100"))));
        assertEquals(0, result.score.compareTo(new BigDecimal("100.0")));
        assertEquals(1, result.trips);
    }

    @Test
    void lateExceptionAndDoubledFreightScoresZero() {
        CarrierScore.Trip trip =
                CarrierScore.trip(
                        true,
                        LocalDateTime.of(2026, 9, 1, 8, 0),
                        LocalDateTime.of(2026, 9, 2, 8, 0),
                        new BigDecimal("100"),
                        new BigDecimal("200"));
        CarrierScore.Result result = CarrierScore.score("JD", Collections.singletonList(trip));
        assertEquals(0, result.score.compareTo(new BigDecimal("0.0")));
        assertTrue(trip.exception);
    }

    @Test
    void missingPromiseTreatsCleanDeliveryAsOnTime() {
        CarrierScore.Trip trip = CarrierScore.trip(false, null, null, null, null);
        assertTrue(trip.onTime);
        CarrierScore.Result result = CarrierScore.score("SELF01", Collections.emptyList());
        assertNull(result.score);
    }

    @Test
    void tieBreaksTowardLowerRate() {
        CarrierScore.Result sf = new CarrierScore.Result("SF", 1, null, null, null, new BigDecimal("80.0"));
        CarrierScore.Result jd = new CarrierScore.Result("JD", 1, null, null, null, new BigDecimal("80.0"));
        CarrierScore.Result none = new CarrierScore.Result("SELF01", 0, null, null, null, null);
        assertEquals("JD", CarrierScore.best(Arrays.asList(sf, none, jd)).carrierCode);
        assertNull(CarrierScore.best(Collections.singletonList(none)));
    }
}
