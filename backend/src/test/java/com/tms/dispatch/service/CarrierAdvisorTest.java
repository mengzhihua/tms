package com.tms.dispatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tms.common.BizException;
import com.tms.common.CarrierRates;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CarrierAdvisorTest {
    @Test
    void fastCheapAndBalancePickDifferentCarriers() {
        CarrierAdvisor.Advice fast = CarrierAdvisor.advise("FAST");
        assertEquals(CarrierRates.SF, fast.carrierCode);
        assertEquals(1, fast.transitDays);
        assertEquals(new BigDecimal("2.2"), fast.rate);

        CarrierAdvisor.Advice cheap = CarrierAdvisor.advise("cheap");
        assertEquals(CarrierRates.SELF01, cheap.carrierCode);
        assertEquals(3, cheap.transitDays);
        assertEquals(new BigDecimal("1.4"), cheap.rate);

        CarrierAdvisor.Advice balance = CarrierAdvisor.advise(null);
        assertEquals(CarrierAdvisor.BALANCE, balance.preference);
        assertEquals(CarrierRates.JD, balance.carrierCode);
        assertEquals(2, balance.transitDays);
        assertEquals(3, balance.options.size());
    }

    @Test
    void unknownPreferenceIsRejected() {
        assertThrows(BizException.class, () -> CarrierAdvisor.advise("RATING"));
    }
}
