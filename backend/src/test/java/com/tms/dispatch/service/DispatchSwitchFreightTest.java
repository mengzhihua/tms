package com.tms.dispatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tms.basic.entity.Carrier;
import com.tms.basic.mapper.CarrierMapper;
import com.tms.basic.mapper.DriverMapper;
import com.tms.basic.mapper.RouteMapper;
import com.tms.basic.mapper.VehicleMapper;
import com.tms.billing.entity.FreightBill;
import com.tms.billing.service.BillingService;
import com.tms.common.CodeGenerator;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.order.service.VolumeService;
import com.tms.thirdparty.ThirdPartyLogisticsGateway;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class DispatchSwitchFreightTest {
    @Test
    void switchCarrierRescalesWaybillAndBillFreight() {
        WaybillMapper waybillMapper = Mockito.mock(WaybillMapper.class);
        TransportOrderMapper orderMapper = Mockito.mock(TransportOrderMapper.class);
        CarrierMapper carrierMapper = Mockito.mock(CarrierMapper.class);
        TrackingEventMapper eventMapper = Mockito.mock(TrackingEventMapper.class);
        BillingService billingService = Mockito.mock(BillingService.class);
        DispatchService dispatch = new DispatchService(
                waybillMapper,
                orderMapper,
                Mockito.mock(VehicleMapper.class),
                Mockito.mock(DriverMapper.class),
                carrierMapper,
                Mockito.mock(RouteMapper.class),
                eventMapper,
                Mockito.mock(CodeGenerator.class),
                Mockito.mock(VolumeService.class),
                billingService,
                Mockito.mock(ThirdPartyLogisticsGateway.class));

        Waybill waybill = new Waybill();
        waybill.setId(1L);
        waybill.setCode("WB-SF");
        waybill.setCarrierCode("SF");
        waybill.setCarrierType("THIRD_PARTY");
        waybill.setStatus("IN_TRANSIT");
        waybill.setFreightAmount(new BigDecimal("110"));
        when(waybillMapper.selectOne(any())).thenReturn(waybill);
        when(waybillMapper.selectById(1L)).thenReturn(waybill);
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(eventMapper.selectList(any())).thenReturn(Collections.emptyList());

        Carrier self = new Carrier();
        self.setCode("SELF01");
        self.setType("SELF");
        when(carrierMapper.selectOne(any())).thenReturn(self);

        FreightBill bill = new FreightBill();
        bill.setId(9L);
        bill.setWaybillId(1L);
        bill.setCarrierCode("SF");
        bill.setAmount(new BigDecimal("110"));
        when(billingService.bills(1L)).thenReturn(Arrays.asList(bill));

        Waybill updated = dispatch.switchCarrierByCode("WB-SF", "SELF01");
        assertEquals("SELF01", updated.getCarrierCode());
        assertEquals(0, new BigDecimal("70.00").compareTo(updated.getFreightAmount()));

        ArgumentCaptor<FreightBill> billCaptor = ArgumentCaptor.forClass(FreightBill.class);
        verify(billingService).updateBill(billCaptor.capture());
        assertEquals("SELF01", billCaptor.getValue().getCarrierCode());
        assertEquals(0, new BigDecimal("70.00").compareTo(billCaptor.getValue().getAmount()));

        ArgumentCaptor<BigDecimal> fromAmount = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> toAmount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(billingService).recordFreightDelta(any(), org.mockito.ArgumentMatchers.eq("SF"),
                fromAmount.capture(), toAmount.capture());
        assertEquals(0, new BigDecimal("110").compareTo(fromAmount.getValue()));
        assertEquals(0, new BigDecimal("70.00").compareTo(toAmount.getValue()));
        assertEquals(0, new BigDecimal("-40.00").compareTo(
                com.tms.billing.service.BillingService.freightDelta(fromAmount.getValue(), toAmount.getValue())));
    }

    @Test
    void syncTrackWithoutThirdPartyClearsExceptionAndRefreshesEta() {
        WaybillMapper waybillMapper = Mockito.mock(WaybillMapper.class);
        TrackingEventMapper eventMapper = Mockito.mock(TrackingEventMapper.class);
        DispatchService dispatch = new DispatchService(
                waybillMapper,
                Mockito.mock(TransportOrderMapper.class),
                Mockito.mock(VehicleMapper.class),
                Mockito.mock(DriverMapper.class),
                Mockito.mock(CarrierMapper.class),
                Mockito.mock(RouteMapper.class),
                eventMapper,
                Mockito.mock(CodeGenerator.class),
                Mockito.mock(VolumeService.class),
                Mockito.mock(BillingService.class),
                Mockito.mock(ThirdPartyLogisticsGateway.class));

        Waybill waybill = new Waybill();
        waybill.setId(2L);
        waybill.setCode("WB-EX");
        waybill.setStatus("DISPATCHED");
        waybill.setExceptionFlag(true);
        waybill.setPlannedArriveTime(LocalDateTime.now().minusHours(8));
        when(waybillMapper.selectOne(any())).thenReturn(waybill);
        when(waybillMapper.selectById(2L)).thenReturn(waybill);
        when(eventMapper.selectList(any())).thenReturn(Collections.emptyList());

        Waybill updated = dispatch.syncTrackByCode("WB-EX");
        assertEquals("IN_TRANSIT", updated.getStatus());
        assertFalse(Boolean.TRUE.equals(updated.getExceptionFlag()));
        assertNotNull(updated.getActualDepartTime());
        assertNotNull(updated.getPlannedArriveTime());
        assertTrue(updated.getPlannedArriveTime().isAfter(LocalDateTime.now()));
        verify(waybillMapper, Mockito.atLeastOnce()).updateById(waybill);
        verify(eventMapper, Mockito.atLeastOnce()).insert(any());
    }

    @Test
    void syncTrackFromCreatedRunsDispatchAndDepart() {
        WaybillMapper waybillMapper = Mockito.mock(WaybillMapper.class);
        TransportOrderMapper orderMapper = Mockito.mock(TransportOrderMapper.class);
        BillingService billingService = Mockito.mock(BillingService.class);
        TrackingEventMapper eventMapper = Mockito.mock(TrackingEventMapper.class);
        DispatchService dispatch = new DispatchService(
                waybillMapper,
                orderMapper,
                Mockito.mock(VehicleMapper.class),
                Mockito.mock(DriverMapper.class),
                Mockito.mock(CarrierMapper.class),
                Mockito.mock(RouteMapper.class),
                eventMapper,
                Mockito.mock(CodeGenerator.class),
                Mockito.mock(VolumeService.class),
                billingService,
                Mockito.mock(ThirdPartyLogisticsGateway.class));

        Waybill waybill = new Waybill();
        waybill.setId(4L);
        waybill.setCode("WB-NEW");
        waybill.setCarrierCode("SELF01");
        waybill.setCarrierType("SELF");
        waybill.setStatus("CREATED");
        waybill.setExceptionFlag(true);
        TransportOrder order = new TransportOrder();
        order.setId(41L);
        order.setCode("TO-NEW");
        order.setStatus("DISPATCHED");
        when(waybillMapper.selectOne(any())).thenReturn(waybill);
        when(waybillMapper.selectById(4L)).thenReturn(waybill);
        when(orderMapper.selectList(any())).thenReturn(Collections.singletonList(order));
        when(billingService.createBill(any(), any(), any())).thenReturn(new BigDecimal("18"));
        when(eventMapper.selectList(any())).thenReturn(Collections.emptyList());

        Waybill updated = dispatch.syncTrackByCode("WB-NEW");
        assertEquals("IN_TRANSIT", updated.getStatus());
        assertNotNull(updated.getActualDepartTime());
        assertEquals(0, new BigDecimal("18").compareTo(updated.getFreightAmount()));
        assertEquals("IN_TRANSIT", order.getStatus());
        verify(billingService).createBill(any(), any(), any());
    }

    @Test
    void dispatchByCodeIsIdempotentForInTransit() {
        WaybillMapper waybillMapper = Mockito.mock(WaybillMapper.class);
        DispatchService dispatch = new DispatchService(
                waybillMapper,
                Mockito.mock(TransportOrderMapper.class),
                Mockito.mock(VehicleMapper.class),
                Mockito.mock(DriverMapper.class),
                Mockito.mock(CarrierMapper.class),
                Mockito.mock(RouteMapper.class),
                Mockito.mock(TrackingEventMapper.class),
                Mockito.mock(CodeGenerator.class),
                Mockito.mock(VolumeService.class),
                Mockito.mock(BillingService.class),
                Mockito.mock(ThirdPartyLogisticsGateway.class));

        Waybill waybill = new Waybill();
        waybill.setId(3L);
        waybill.setCode("WB-LIVE");
        waybill.setStatus("IN_TRANSIT");
        when(waybillMapper.selectOne(any())).thenReturn(waybill);
        when(waybillMapper.selectById(3L)).thenReturn(waybill);

        Waybill updated = dispatch.dispatchByCode("WB-LIVE");
        assertEquals("IN_TRANSIT", updated.getStatus());
    }
}
