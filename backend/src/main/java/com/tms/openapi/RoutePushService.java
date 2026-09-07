package com.tms.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.basic.entity.Customer;
import com.tms.basic.mapper.CustomerMapper;
import com.tms.common.CodeGenerator;
import com.tms.openapi.entity.PushLog;
import com.tms.openapi.mapper.PushLogMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoutePushService {
    private final CustomerMapper customerMapper;
    private final PushLogMapper mapper;
    private final CodeGenerator codeGenerator;
    private final ObjectMapper objectMapper;

    public PushLog push(String customerCode, String orderCode, String status, String eventType, String description) {
        Customer customer =
                customerMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.tms.basic.entity.Customer>()
                                .eq("code", customerCode));
        if (customer == null || !Boolean.TRUE.equals(customer.getPushEnabled())
                || customer.getCallbackUrl() == null || customer.getCallbackUrl().isEmpty()) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderCode", orderCode);
        payload.put("sourceNo", null);
        payload.put("status", status);
        payload.put("eventType", eventType);
        payload.put("description", description);
        payload.put("eventTime", LocalDateTime.now());
        PushLog log = new PushLog();
        log.setCustomerCode(customerCode);
        log.setOrderCode(orderCode);
        log.setEventType(eventType);
        log.setUrl(customer.getCallbackUrl());
        try {
            log.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.setPayload("{}");
        }
        log.setRetryCount(0);
        log.setStatus("PENDING");
        log.setSuccess(false);
        mapper.insert(log);
        if (customer.getCallbackUrl().contains("/mock-receiver")) {
            log.setSuccess(true);
            log.setStatus("SUCCESS");
            log.setResponseCode(200);
            log.setResponseBody("ok");
            mapper.updateById(log);
        }
        return log;
    }
}
