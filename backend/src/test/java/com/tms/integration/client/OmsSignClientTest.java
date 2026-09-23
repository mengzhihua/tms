package com.tms.integration.client;

import com.tms.order.entity.TransportOrder;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OmsSignClientTest {
    @Test
    void deliveredOrderCarriesSourceNo() {
        TransportOrder order = new TransportOrder();
        order.setCode("TO-1");
        order.setSourceNo("SO-1");
        order.setStatus("DELIVERED");
        order.setPodRemark("本人签收");
        Map<String, Object> body = OmsSignClient.payload(order);
        assertEquals("SO-1", body.get("orderNo"));
        assertEquals("TO-1", body.get("tmsOrderNo"));
        assertEquals("本人签收", body.get("remark"));
    }

    @Test
    void exceptionOrMissingSourceIsNotPushed() {
        TransportOrder exception = new TransportOrder();
        exception.setSourceNo("SO-1");
        exception.setStatus("EXCEPTION");
        assertNull(OmsSignClient.payload(exception));

        TransportOrder local = new TransportOrder();
        local.setStatus("DELIVERED");
        assertNull(OmsSignClient.payload(local));
    }
}
