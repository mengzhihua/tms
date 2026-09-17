package com.tms.dispatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.order.service.VolumeService;
import com.tms.thirdparty.ThirdPartyLogisticsGateway;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.math.BigDecimal;
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
    }
}
