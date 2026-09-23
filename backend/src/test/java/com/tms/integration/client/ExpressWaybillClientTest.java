package com.tms.integration.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.common.BizException;
import com.tms.dispatch.entity.Waybill;
import com.tms.order.entity.TransportOrder;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpressWaybillClientTest {
    @Test
    void payloadCarriesCarrierAndOrders() {
        Waybill waybill = new Waybill();
        waybill.setCode("WB-1");
        waybill.setCarrierCode("SF");
        waybill.setFromSiteCode("WH01");
        TransportOrder order = new TransportOrder();
        order.setCode("TO-1");
        Map<String, Object> body = ExpressWaybillClient.payload(waybill, Collections.singletonList(order));
        assertEquals("SF", body.get("carrierCode"));
        assertEquals(Collections.singletonList("TO-1"), body.get("orderNos"));
        assertEquals("SF", body.get("profile"));
        assertEquals(Boolean.TRUE, body.get("sandbox"));
        assertEquals("WB-1", body.get("orderId"));
        assertEquals("1", body.get("expressType"));
    }

    @Test
    void jdAndOtherCarriersKeepSandboxPayload() {
        Waybill jd = new Waybill();
        jd.setCode("WB-2");
        jd.setCarrierCode("jd");
        Map<String, Object> jdBody = ExpressWaybillClient.payload(jd, Collections.<TransportOrder>emptyList());
        assertEquals("JD", jdBody.get("profile"));
        assertEquals(Boolean.TRUE, jdBody.get("sandbox"));
        assertEquals("1", jdBody.get("promiseTimeType"));
        Waybill self = new Waybill();
        self.setCarrierCode("SELF01");
        Map<String, Object> generic = ExpressWaybillClient.payload(self, null);
        assertEquals("GENERIC", generic.get("profile"));
        assertEquals(Boolean.TRUE, generic.get("sandbox"));
        assertNull(generic.get("expressType"));
    }

    @Test
    void acceptedResponseReturnsTrackingNo() throws Exception {
        String no =
                ExpressWaybillClient.trackingNo(
                        new ObjectMapper().readTree("{\"code\":0,\"data\":{\"trackingNo\":\"SF1001\"}}"));
        assertEquals("SF1001", no);
    }

    @Test
    void carrierAddressBeatsGlobalAddress() {
        assertEquals("http://carrier.example", ExpressWaybillClient.chooseBase(" http://carrier.example ", "http://global"));
        assertEquals("http://global", ExpressWaybillClient.chooseBase(" ", "http://global"));
        assertNull(ExpressWaybillClient.chooseBase(null, " "));
        assertEquals("http://carrier.example/api/open/waybill", ExpressWaybillClient.endpoint("http://carrier.example/"));
        assertThrows(BizException.class, () -> ExpressWaybillClient.endpoint(null));
    }

    @Test
    void rejectedResponseFails() {
        assertThrows(
                BizException.class,
                () ->
                        ExpressWaybillClient.trackingNo(
                                new ObjectMapper().readTree("{\"code\":400,\"msg\":\"拒绝\"}")));
    }
}
