package com.tms.integration.controller;

import com.tms.billing.entity.FreightBill;
import com.tms.billing.mapper.FreightBillMapper;
import com.tms.common.BizException;
import com.tms.common.R;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.dispatch.service.DispatchService;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** IR 控制塔按运单号调度、拉轨迹、换承运商。 */
@RestController
@RequestMapping("/api/open/ir")
@RequiredArgsConstructor
public class OpenIrController {
    private final DispatchService dispatchService;
    private final WaybillMapper waybillMapper;
    private final TransportOrderMapper transportOrderMapper;
    private final FreightBillMapper freightBillMapper;
    private final ConcurrentHashMap<String, Object> actionCache = new ConcurrentHashMap<String, Object>();

    @Value("${tms.open.api-key:tms-open-key}")
    private String apiKey;

    @Data
    public static class CodeReq {
        private String waybillCode;
        private String targetKey;
        private String carrierCode;
        private String type;
        private String idempotencyKey;
        private Map<String, Object> params;
    }

    @GetMapping("/snapshots")
    public R<Map<String, Object>> snapshots(
            @RequestHeader(value = "X-Api-Key", required = false) String key) {
        checkKey(key);
        Map<Long, String> sourceByWaybill = new LinkedHashMap<Long, String>();
        Map<String, String> sourceByCode = new LinkedHashMap<String, String>();
        for (TransportOrder order : transportOrderMapper.selectList(null)) {
            if (order.getSourceNo() == null || order.getSourceNo().trim().isEmpty()) {
                continue;
            }
            if (order.getWaybillId() != null && !sourceByWaybill.containsKey(order.getWaybillId())) {
                sourceByWaybill.put(order.getWaybillId(), order.getSourceNo());
            }
            if (order.getWaybillCode() != null && !sourceByCode.containsKey(order.getWaybillCode())) {
                sourceByCode.put(order.getWaybillCode(), order.getSourceNo());
            }
        }
        java.util.Set<String> waybillCodes = new java.util.HashSet<String>();
        List<Map<String, Object>> waybills = new ArrayList<Map<String, Object>>();
        for (Waybill waybill : waybillMapper.selectList(null)) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            String sourceNo = sourceByWaybill.get(waybill.getId());
            if (sourceNo == null) {
                sourceNo = sourceByCode.get(waybill.getCode());
            }
            row.put("waybillCode", waybill.getCode());
            row.put("waybillNo", waybill.getCode());
            row.put("code", waybill.getCode());
            row.put("sourceNo", sourceNo);
            row.put("sourceOrderNo", sourceNo);
            row.put("carrierCode", waybill.getCarrierCode());
            row.put("carrier", waybill.getCarrierCode());
            row.put("status", waybill.getStatus());
            row.put("fromSiteCode", waybill.getFromSiteCode());
            row.put("fromSite", waybill.getFromSiteCode());
            row.put("plannedArriveTime", waybill.getPlannedArriveTime());
            row.put("planArriveTime", waybill.getPlannedArriveTime());
            row.put("actualArriveTime", waybill.getActualArriveTime());
            row.put("arriveTime", waybill.getActualArriveTime());
            row.put("freightAmount", waybill.getFreightAmount());
            row.put("amount", waybill.getFreightAmount());
            row.put("exceptionFlag", waybill.getExceptionFlag());
            waybills.add(row);
            if (waybill.getCode() != null) {
                waybillCodes.add(waybill.getCode());
            }
        }
        for (TransportOrder order : transportOrderMapper.selectList(null)) {
            if (!"CREATED".equals(order.getStatus()) || order.getCode() == null) {
                continue;
            }
            if (waybillCodes.contains(order.getCode())
                    || (order.getWaybillCode() != null && waybillCodes.contains(order.getWaybillCode()))) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("waybillCode", order.getCode());
            row.put("waybillNo", order.getCode());
            row.put("code", order.getCode());
            row.put("sourceNo", order.getSourceNo());
            row.put("sourceOrderNo", order.getSourceNo());
            row.put("carrierCode", null);
            row.put("carrier", null);
            row.put("status", "CREATED");
            row.put("fromSiteCode", order.getFromSiteCode());
            row.put("fromSite", order.getFromSiteCode());
            row.put("plannedArriveTime", order.getRequiredDeliveryTime());
            row.put("planArriveTime", order.getRequiredDeliveryTime());
            row.put("actualArriveTime", null);
            row.put("arriveTime", null);
            row.put("freightAmount", null);
            row.put("amount", null);
            row.put("exceptionFlag", Boolean.FALSE);
            waybills.add(row);
            waybillCodes.add(order.getCode());
        }
        List<Map<String, Object>> bills = new ArrayList<Map<String, Object>>();
        for (FreightBill bill : freightBillMapper.selectList(null)) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("bizDate", bill.getCreatedAt() == null ? null : bill.getCreatedAt().toLocalDate().toString());
            row.put("billingDate", bill.getCreatedAt() == null ? null : bill.getCreatedAt().toLocalDate().toString());
            row.put("orderNo", bill.getOrderCode());
            row.put("sourceNo", bill.getOrderCode());
            row.put("carrierCode", bill.getCarrierCode());
            row.put("carrier", bill.getCarrierCode());
            row.put("freightAmount", bill.getAmount());
            row.put("amount", bill.getAmount());
            bills.add(row);
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("system", "TMS");
        payload.put("waybills", waybills);
        payload.put("bills", bills);
        return R.ok(payload);
    }

    @PostMapping("/dispatch")
    public R<Waybill> dispatch(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestBody CodeReq req) {
        checkKey(key);
        return R.ok(dispatchService.dispatchByCode(code(req)));
    }

    @PostMapping("/sync-track")
    public R<Waybill> syncTrack(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestBody CodeReq req) {
        checkKey(key);
        return R.ok(dispatchService.syncTrackByCode(code(req)));
    }

    @PostMapping("/switch-carrier")
    public R<Waybill> switchCarrier(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestBody CodeReq req) {
        checkKey(key);
        return R.ok(dispatchService.switchCarrierByCode(code(req), carrier(req)));
    }

    @PostMapping("/actions")
    public R<Waybill> actions(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestBody CodeReq req) {
        checkKey(key);
        String type = req.getType() == null ? "" : req.getType();
        return R.ok((Waybill) executeOnce(cacheKey(type, code(req), req.getIdempotencyKey()), () -> {
            if ("TMS_DISPATCH".equals(type)) {
                return dispatchService.dispatchByCode(code(req), carrier(req));
            }
            if ("TMS_SYNC_TRACK".equals(type)) {
                return dispatchService.syncTrackByCode(code(req));
            }
            if ("TMS_SWITCH_CARRIER".equals(type)) {
                return dispatchService.switchCarrierByCode(code(req), carrier(req));
            }
            throw new BizException("不支持的 IR 指令: " + type);
        }));
    }

    private Object executeOnce(String cacheKey, Supplier<Object> work) {
        if (cacheKey == null) {
            return work.get();
        }
        Object cached = actionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        synchronized (actionCache) {
            cached = actionCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
            Object created = work.get();
            actionCache.put(cacheKey, created);
            return created;
        }
    }

    private static String cacheKey(String type, String targetKey, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty() || "null".equals(idempotencyKey)) {
            return null;
        }
        return type + "|" + (targetKey == null ? "" : targetKey) + "|" + idempotencyKey.trim();
    }

    private String code(CodeReq req) {
        String value = first(req.getWaybillCode(), req.getTargetKey());
        if (value == null) {
            throw new BizException("waybillCode 必填");
        }
        return value;
    }

    private String carrier(CodeReq req) {
        String value = req.getCarrierCode();
        if ((value == null || value.trim().isEmpty()) && req.getParams() != null) {
            Object param = req.getParams().get("carrierCode");
            value = param == null ? null : String.valueOf(param);
        }
        return value;
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

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && !"null".equals(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
