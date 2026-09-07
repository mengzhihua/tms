package com.tms.exc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tms.common.CodeGenerator;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.exc.entity.TransportException;
import com.tms.exc.mapper.TransportExceptionMapper;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SlaMonitorTest {
    @Test
    void scansDepartArriveAndSignTimeouts() {
        TransportExceptionMapper exceptionMapper = Mockito.mock(TransportExceptionMapper.class);
        WaybillMapper waybillMapper = Mockito.mock(WaybillMapper.class);
        TrackingEventMapper eventMapper = Mockito.mock(TrackingEventMapper.class);
        Waybill depart = waybill("DISPATCHED");
        depart.setPlannedDepartTime(LocalDateTime.of(2025, 1, 1, 8, 0));
        Waybill arrive = waybill("IN_TRANSIT");
        arrive.setPromisedArriveTime(LocalDateTime.of(2025, 1, 1, 8, 0));
        Waybill sign = waybill("ARRIVED");
        sign.setActualArriveTime(LocalDateTime.of(2025, 1, 1, 8, 0));
        Mockito.when(waybillMapper.selectList(Mockito.isNull()))
                .thenReturn(Arrays.asList(depart, arrive, sign));
        Mockito.when(exceptionMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        ExceptionService service =
                new ExceptionService(
                        exceptionMapper,
                        waybillMapper,
                        eventMapper,
                        new CodeGenerator());

        int created = service.scan(LocalDateTime.of(2025, 1, 2, 12, 0));

        assertEquals(3, created);
        Mockito.verify(exceptionMapper, Mockito.times(3)).insert(any(TransportException.class));
    }

    private Waybill waybill(String status) {
        Waybill waybill = new Waybill();
        waybill.setId((long) status.hashCode());
        waybill.setCode(status);
        waybill.setStatus(status);
        waybill.setCarrierCode("SELF01");
        return waybill;
    }
}
