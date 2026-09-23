package com.tms.integration.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.common.BizException;
import com.tms.dispatch.entity.Waybill;
import com.tms.order.entity.TransportOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** 电子面单取号。默认 mock，由承运商适配器本地编号。http 时向配置的地址实连取号。 */
@Component
public class ExpressWaybillClient {
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tms.express.mode:mock}")
    private String mode;
    @Value("${tms.express.base-url:}")
    private String baseUrl;
    @Value("${tms.express.api-key:}")
    private String apiKey;

    /** mock 返回 null，调用方继续走本地适配器。 */
    public String issue(Waybill waybill, List<TransportOrder> orders) {
        if (!"http".equalsIgnoreCase(mode)) {
            return null;
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new BizException("电子面单地址未配置");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.set("X-Api-Key", apiKey.trim());
        }
        try {
            String response =
                    rest.postForObject(
                            baseUrl.replaceAll("/$", "") + "/api/open/waybill",
                            new HttpEntity<Object>(payload(waybill, orders), headers),
                            String.class);
            return trackingNo(objectMapper.readTree(response == null ? "{}" : response));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("电子面单取号失败: " + e.getMessage());
        }
    }

    public static Map<String, Object> payload(Waybill waybill, List<TransportOrder> orders) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("carrierCode", waybill == null ? null : waybill.getCarrierCode());
        body.put("waybillCode", waybill == null ? null : waybill.getCode());
        body.put("fromSiteCode", waybill == null ? null : waybill.getFromSiteCode());
        List<String> orderNos = new ArrayList<String>();
        if (orders != null) {
            for (TransportOrder order : orders) {
                if (order != null && order.getCode() != null) {
                    orderNos.add(order.getCode());
                }
            }
        }
        body.put("orderNos", orderNos);
        return body;
    }

    public static String trackingNo(JsonNode root) {
        if (root == null || root.path("code").asInt(-1) != 0) {
            throw new BizException("电子面单被拒绝: " + root);
        }
        String no = root.path("data").path("trackingNo").asText("").trim();
        if (no.isEmpty()) {
            throw new BizException("电子面单没有运单号");
        }
        return no;
    }
}
