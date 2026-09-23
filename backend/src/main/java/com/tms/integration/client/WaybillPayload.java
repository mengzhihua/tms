package com.tms.integration.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按承运商编码组装取号报文。顺丰、京东带上各自常用字段，其余用通用格式。
 * sandbox 固定为 true：报文发到配置地址，不连承运商生产环境。
 */
public final class WaybillPayload {
    private WaybillPayload() {}

    public static Map<String, Object> shape(String carrierCode, String waybillCode, String fromSiteCode, List<String> orderNos) {
        String code = carrierCode == null ? "" : carrierCode.trim().toUpperCase();
        String profile = "SF".equals(code) ? "SF" : "JD".equals(code) ? "JD" : "GENERIC";
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("profile", profile);
        body.put("sandbox", Boolean.TRUE);
        body.put("carrierCode", carrierCode);
        body.put("waybillCode", waybillCode);
        body.put("fromSiteCode", fromSiteCode);
        body.put("orderNos", orderNos);
        if ("SF".equals(profile)) {
            body.put("orderId", waybillCode);
            body.put("expressType", "1");
        } else if ("JD".equals(profile)) {
            body.put("orderId", waybillCode);
            body.put("promiseTimeType", "1");
        }
        return body;
    }
}
