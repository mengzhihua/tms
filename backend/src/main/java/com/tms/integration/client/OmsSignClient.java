package com.tms.integration.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.common.BizException;
import com.tms.order.entity.TransportOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** 正常签收后通知 OMS 完成订单。异常签收不推。默认 mock。 */
@Component
public class OmsSignClient {
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tms.oms.mode:mock}")
    private String mode;
    @Value("${tms.oms.base-url:http://localhost:8081}")
    private String baseUrl;
    @Value("${tms.oms.api-key:oms-open-key}")
    private String apiKey;

    public void push(TransportOrder order) {
        Map<String, Object> body = payload(order);
        if (body == null || !"http".equalsIgnoreCase(mode)) {
            return;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Api-Key", apiKey);
        try {
            String response = rest.postForObject(baseUrl + "/api/open/tms/signed",
                    new HttpEntity<Object>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            if (root.path("code").asInt(-1) != 0) {
                throw new BizException("OMS 拒绝签收回传: " + response);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("推送 OMS 签收失败: " + e.getMessage());
        }
    }

    /** 只回传已送达且带来源单号的运输订单。 */
    public static Map<String, Object> payload(TransportOrder order) {
        if (order == null || order.getSourceNo() == null || order.getSourceNo().trim().isEmpty()) {
            return null;
        }
        if (!"DELIVERED".equals(order.getStatus())) {
            return null;
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("orderNo", order.getSourceNo().trim());
        body.put("tmsOrderNo", order.getCode());
        body.put("remark", order.getPodRemark() == null || order.getPodRemark().trim().isEmpty()
                ? "TMS 签收" : order.getPodRemark().trim());
        return body;
    }
}
