package com.tms.integration.controller;

import com.tms.common.BizException;
import com.tms.common.R;
import com.tms.order.entity.TransportOrder;
import com.tms.order.entity.TransportOrderLine;
import com.tms.order.service.TransportOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OMS 发货后创建运输订单。同一来源单号重复推送返回已有订单。
 */
@RestController
@RequestMapping("/api/open")
@RequiredArgsConstructor
public class OpenTransportController {
    private final TransportOrderService orders;

    @Value("${tms.open.api-key:tms-open-key}")
    private String apiKey;

    @PostMapping("/transport-order")
    public R<TransportOrder> create(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestBody Map<String, Object> body) {
        checkKey(key);
        String orderNo = text(body.get("orderNo"));
        if (orderNo == null) {
            throw new BizException("orderNo 不能为空");
        }
        TransportOrder existing = orders.findBySourceNo(orderNo);
        if (existing != null) {
            return R.ok(existing);
        }
        TransportOrder order = new TransportOrder();
        order.setSourceNo(orderNo);
        order.setOrderType("SALES");
        order.setCustomerCode(first(text(body.get("customerCode")), "OMS"));
        order.setConsigneeName(text(body.get("receiverName")));
        order.setConsigneePhone(text(body.get("receiverPhone")));
        order.setConsigneeAddress(text(body.get("address")));
        order.setPriority(5);
        order.setLines(lines(body.get("items"), text(body.get("trackingNo"))));
        return R.ok(orders.create(order));
    }

    @SuppressWarnings("unchecked")
    static List<TransportOrderLine> lines(Object raw, String trackingNo) {
        List<TransportOrderLine> lines = new ArrayList<>();
        if (raw instanceof List) {
            for (Object row : (List<?>) raw) {
                if (!(row instanceof Map)) {
                    continue;
                }
                Map<String, Object> item = (Map<String, Object>) row;
                TransportOrderLine line = new TransportOrderLine();
                line.setItemCode(first(text(item.get("sku")), text(item.get("itemCode")), "GOODS"));
                line.setItemName(first(text(item.get("name")), line.getItemCode()));
                line.setQty(qty(item.get("qty")));
                line.setWeightKg(new BigDecimal("1"));
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            TransportOrderLine line = new TransportOrderLine();
            line.setItemCode("GOODS");
            line.setItemName(trackingNo == null ? "OMS 发货" : trackingNo);
            line.setQty(BigDecimal.ONE);
            line.setWeightKg(new BigDecimal("1"));
            lines.add(line);
        }
        return lines;
    }

    private void checkKey(String given) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return;
        }
        if (given == null || !MessageDigest.isEqual(
                given.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new BizException("开放接口 X-Api-Key 无效");
        }
    }

    private static BigDecimal qty(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            return new BigDecimal(String.valueOf(value).trim());
        }
        return BigDecimal.ONE;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equals(text) ? null : text;
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
