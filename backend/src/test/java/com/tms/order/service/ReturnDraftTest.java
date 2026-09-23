package com.tms.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tms.common.BizException;
import com.tms.order.entity.TransportOrder;
import com.tms.order.entity.TransportOrderLine;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ReturnDraftTest {
    @Test
    void deliveredOrderBecomesReturnWithPartiesSwapped() {
        TransportOrder forward = new TransportOrder();
        forward.setCode("TO-1");
        forward.setStatus("DELIVERED");
        forward.setCustomerCode("CUS01");
        forward.setFromSiteCode("WH01");
        forward.setConsigneeName("苏州客户");
        forward.setConsigneeAddress("苏州客户地址");
        TransportOrderLine line = new TransportOrderLine();
        line.setItemCode("SKU001");
        line.setQty(new BigDecimal("2"));
        forward.setLines(Collections.singletonList(line));

        TransportOrder back = TransportOrderService.returnDraft(forward);
        assertEquals("RETURN", back.getOrderType());
        assertEquals("TO-1", back.getSourceNo());
        assertEquals("苏州客户", back.getConsignorName());
        assertEquals("WH01", back.getConsigneeName());
        assertEquals("WH01", back.getConsigneeAddress());
        assertEquals(new BigDecimal("2"), back.getLines().get(0).getQty());
    }

    @Test
    void unfinishedOrderCannotOpenReturn() {
        TransportOrder created = new TransportOrder();
        created.setStatus("CREATED");
        assertThrows(BizException.class, () -> TransportOrderService.returnDraft(created));
    }
}
