package com.tms.openapi;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.basic.entity.Customer;
import com.tms.basic.mapper.CustomerMapper;
import com.tms.dispatch.entity.Waybill;
import com.tms.openapi.entity.PushLog;
import com.tms.openapi.mapper.PushLogMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class RoutePushService {
    private final CustomerMapper customerMapper;
    private final PushLogMapper mapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public PushLog push(
            String customerCode,
            String orderCode,
            String status,
            String eventType,
            String description) {
        return push(customerCode, orderCode, null, status, eventType, description, null);
    }

    public PushLog push(
            String customerCode,
            String orderCode,
            String status,
            String eventType,
            String description,
            Waybill waybill) {
        return push(customerCode, orderCode, null, status, eventType, description, waybill);
    }

    public PushLog push(
            String customerCode,
            String orderCode,
            String sourceNo,
            String status,
            String eventType,
            String description,
            Waybill waybill) {
        Customer customer =
                customerMapper.selectOne(new QueryWrapper<Customer>().eq("code", customerCode));
        if (customer == null
                || !Boolean.TRUE.equals(customer.getPushEnabled())
                || customer.getCallbackUrl() == null
                || customer.getCallbackUrl().isEmpty()) {
            return null;
        }
        PushLog log = new PushLog();
        log.setCustomerCode(customerCode);
        log.setOrderCode(orderCode);
        log.setEventType(eventType);
        log.setUrl(customer.getCallbackUrl());
        log.setRetryCount(0);
        log.setStatus("PENDING");
        log.setSuccess(false);
        log.setPayload(
                payload(
                        orderCode,
                        sourceNo,
                        status,
                        eventType,
                        description,
                        waybill));
        mapper.insert(log);
        attempt(log);
        return log;
    }

    @Scheduled(fixedDelay = 60000)
    public void retryDue() {
        List<PushLog> logs =
                mapper.selectList(
                        new QueryWrapper<PushLog>()
                                .eq("status", "PENDING")
                                .le("next_retry_time", LocalDateTime.now()));
        for (PushLog log : logs) {
            attempt(log);
        }
    }

    public PushLog retry(Long id) {
        PushLog log = mapper.selectById(id);
        if (log == null) {
            return null;
        }
        log.setRetryCount(0);
        log.setStatus("PENDING");
        log.setSuccess(false);
        log.setNextRetryTime(null);
        mapper.updateById(log);
        attempt(log);
        return log;
    }

    private void attempt(PushLog log) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            log.getUrl(),
                            new HttpEntity<>(log.getPayload(), headers),
                            String.class);
            log.setResponseCode(response.getStatusCodeValue());
            log.setResponseBody(response.getBody());
            if (response.getStatusCode().is2xxSuccessful()) {
                log.setSuccess(true);
                log.setStatus("SUCCESS");
                log.setNextRetryTime(null);
            } else {
                fail(log);
            }
        } catch (RuntimeException ex) {
            log.setResponseCode(null);
            log.setResponseBody(ex.getMessage());
            fail(log);
        }
        mapper.updateById(log);
    }

    private void fail(PushLog log) {
        int retryCount = log.getRetryCount() == null ? 0 : log.getRetryCount();
        retryCount++;
        log.setRetryCount(retryCount);
        log.setSuccess(false);
        long minutes = retryCount == 1 ? 1 : retryCount == 2 ? 5 : 30;
        if (retryCount >= 3) {
            log.setStatus("FAILED");
            log.setNextRetryTime(LocalDateTime.now().plusMinutes(minutes));
            return;
        }
        log.setStatus("PENDING");
        log.setNextRetryTime(LocalDateTime.now().plusMinutes(minutes));
    }

    private String payload(
            String orderCode,
            String sourceNo,
            String status,
            String eventType,
            String description,
            Waybill waybill) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderCode", orderCode);
        payload.put("sourceNo", sourceNo);
        payload.put("status", status);
        payload.put("eventType", eventType);
        payload.put("description", description);
        payload.put("eventTime", LocalDateTime.now());
        payload.put("waybillCode", waybill == null ? null : waybill.getCode());
        payload.put("thirdPartyNo", waybill == null ? null : waybill.getThirdPartyNo());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
