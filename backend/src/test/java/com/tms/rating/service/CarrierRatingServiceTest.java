package com.tms.rating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tms.basic.entity.Carrier;
import com.tms.basic.mapper.CarrierMapper;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.exc.mapper.TransportExceptionMapper;
import com.tms.pod.mapper.PodReceiptMapper;
import com.tms.rating.entity.CarrierRating;
import com.tms.rating.mapper.CarrierRatingMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CarrierRatingServiceTest {
    @Test
    void computesScoreAndGrade() {
        WaybillMapper waybillMapper = Mockito.mock(WaybillMapper.class);
        CarrierMapper carrierMapper = Mockito.mock(CarrierMapper.class);
        TransportExceptionMapper exceptionMapper = Mockito.mock(TransportExceptionMapper.class);
        PodReceiptMapper podMapper = Mockito.mock(PodReceiptMapper.class);
        CarrierRatingMapper ratingMapper = Mockito.mock(CarrierRatingMapper.class);
        Carrier carrier = new Carrier();
        carrier.setCode("SELF01");
        carrier.setName("自建车队");
        Waybill waybill = new Waybill();
        waybill.setId(1L);
        waybill.setStatus("CLOSED");
        waybill.setActualDepartTime(LocalDateTime.of(2025, 1, 1, 8, 0));
        waybill.setActualArriveTime(LocalDateTime.of(2025, 1, 1, 10, 0));
        waybill.setOnTime(true);
        Mockito.when(carrierMapper.selectList(Mockito.isNull())).thenReturn(Arrays.asList(carrier));
        Mockito.when(waybillMapper.selectList(Mockito.any())).thenReturn(Arrays.asList(waybill));
        Mockito.when(ratingMapper.selectOne(Mockito.any())).thenReturn(null);
        Mockito.when(exceptionMapper.selectCount(Mockito.any())).thenReturn(0L);
        Mockito.when(podMapper.selectCount(Mockito.any())).thenReturn(1L, 1L);

        CarrierRatingService service =
                new CarrierRatingService(
                        waybillMapper,
                        carrierMapper,
                        exceptionMapper,
                        podMapper,
                        ratingMapper);

        CarrierRating result = service.compute("2025-01").get(0);

        assertEquals(new BigDecimal("100.0"), result.getScore());
        assertEquals("A", result.getGrade());
    }
}
