package com.tms.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.billing.entity.FreightBill;
import com.tms.common.BizException;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** 把运费差额推到 BMS。默认 mock，不外呼。 */
@Component
public class BmsFreightClient {
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tms.bms.mode:mock}")
    private String mode;
    @Value("${tms.bms.base-url:http://localhost:8084}")
    private String baseUrl;
    @Value("${tms.bms.api-key:bms-open-key}")
    private String apiKey;

    public void push(FreightBill bill) {
        if (bill == null || !"http".equalsIgnoreCase(mode)) {
            return;
        }
        Map<String, Object> doc = BillingService.bmsDoc(bill);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);
        try {
            String body = rest.postForObject(baseUrl + "/api/open/tms/docs",
                    new HttpEntity<Object>(Collections.singletonList(doc), headers), String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            if (root.path("code").asInt(-1) != 0) {
                throw new BizException("BMS 拒绝运费差额: " + body);
            }
            JsonNode error = root.path("data").path(0).path("error");
            if (!error.isMissingNode() && !error.isNull() && !error.asText().isEmpty()) {
                throw new BizException("BMS 拒绝运费差额: " + error.asText());
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("推送 BMS 运费差额失败: " + e.getMessage());
        }
    }
}
