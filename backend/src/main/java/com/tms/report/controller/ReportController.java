package com.tms.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tms.common.R;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.exc.entity.TransportException;
import com.tms.exc.mapper.TransportExceptionMapper;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.tracking.mapper.GeofenceAlertMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private final WaybillMapper waybillMapper;
    private final TransportOrderMapper orderMapper;
    private final TransportExceptionMapper exceptionMapper;
    private final GeofenceAlertMapper alertMapper;

    @GetMapping("/sla")
    public R<List<Map<String, Object>>> sla(@RequestParam(required = false) String carrierCode) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Waybill waybill : waybills(carrierCode)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("carrier", waybill.getCarrierCode());
            item.put("serviceLevel", waybill.getServiceLevelCode());
            item.put("waybillCount", 1);
            item.put("onTimeCount", Boolean.TRUE.equals(waybill.getOnTime()) ? 1 : 0);
            item.put("onTimeRate", Boolean.TRUE.equals(waybill.getOnTime()) ? 1 : 0);
            item.put("avgTransitHours", transitHours(waybill));
            result.add(item);
        }
        return R.ok(result);
    }

    @GetMapping("/sla-flow")
    public R<List<Map<String, Object>>> slaFlow(@RequestParam(required = false) String carrierCode) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TransportOrder order : orderMapper.selectList(null)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fromSiteCode", order.getFromSiteCode());
            item.put("consigneeProvince", order.getConsigneeProvince());
            item.put("count", 1);
            result.add(item);
        }
        return R.ok(result);
    }

    @GetMapping("/transit-sign")
    public R<List<Map<String, Object>>> transitSign(@RequestParam(required = false) String carrierCode) {
        Map<LocalDate, Map<String, Object>> daily = new LinkedHashMap<>();
        for (Waybill waybill : waybills(carrierCode)) {
            LocalDate date =
                    waybill.getActualDepartTime() == null
                            ? LocalDate.now()
                            : waybill.getActualDepartTime().toLocalDate();
            Map<String, Object> item = daily.get(date);
            if (item == null) {
                item = new LinkedHashMap<>();
                item.put("date", date);
                item.put("departed", 0);
                item.put("arrived", 0);
                item.put("signed", 0);
                item.put("inTransitEnd", 0);
                daily.put(date, item);
            }
            item.put("departed", ((Integer) item.get("departed")) + 1);
            if (waybill.getActualArriveTime() != null) {
                item.put("arrived", ((Integer) item.get("arrived")) + 1);
            }
            if ("DELIVERED".equals(waybill.getStatus()) || "CLOSED".equals(waybill.getStatus())) {
                item.put("signed", ((Integer) item.get("signed")) + 1);
            }
        }
        return R.ok(new ArrayList<>(daily.values()));
    }

    @GetMapping("/quality")
    public R<List<Map<String, Object>>> quality(@RequestParam(required = false) String carrierCode) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (TransportException exception : exceptionMapper.selectList(null)) {
            if (carrierCode != null && !carrierCode.equals(exception.getCarrierCode())) {
                continue;
            }
            Map<String, Object> item = grouped.get(exception.getCarrierCode());
            if (item == null) {
                item = new LinkedHashMap<>();
                item.put("carrier", exception.getCarrierCode());
                item.put("waybillCount", 0);
                item.put("exceptionCount", 0);
                item.put("exceptionRate", 0);
                item.put("byType", new LinkedHashMap<String, Integer>());
                item.put("claimAmount", 0);
                item.put("podReturnRate", 0);
                item.put("alertCount", 0);
                grouped.put(exception.getCarrierCode(), item);
            }
            item.put("exceptionCount", ((Integer) item.get("exceptionCount")) + 1);
            @SuppressWarnings("unchecked")
            Map<String, Integer> byType = (Map<String, Integer>) item.get("byType");
            byType.put(exception.getType(), byType.getOrDefault(exception.getType(), 0) + 1);
        }
        return R.ok(new ArrayList<>(grouped.values()));
    }

    @GetMapping("/order-structure")
    public R<Map<String, Object>> orderStructure() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byType", groupOrders("orderType"));
        result.put("byCustomer", groupOrders("customerCode"));
        result.put("byCarrier", groupOrders("carrierCode"));
        result.put("byServiceLevel", groupOrders("serviceLevelCode"));
        result.put("byRegion", groupOrders("regionCode"));
        return R.ok(result);
    }

    @GetMapping("/alert-summary")
    public R<Map<String, Object>> alertSummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(
                "openExceptions",
                exceptionMapper.selectCount(
                        new QueryWrapper<TransportException>().ne("status", "CLOSED")));
        result.put(
                "unhandledGeofenceAlerts",
                alertMapper.selectCount(new QueryWrapper<com.tms.tracking.entity.GeofenceAlert>().eq("handled", false)));
        result.put(
                "imminentWaybills",
                waybillMapper.selectList(
                        new QueryWrapper<Waybill>()
                                .eq("status", "IN_TRANSIT")
                                .ge("promised_arrive_time", LocalDateTime.now())
                                .le("promised_arrive_time", LocalDateTime.now().plusHours(2))));
        return R.ok(result);
    }

    private List<Waybill> waybills(String carrierCode) {
        return waybillMapper.selectList(new QueryWrapper<Waybill>().eq(carrierCode != null, "carrier_code", carrierCode));
    }

    private Object groupOrders(String field) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (TransportOrder order : orderMapper.selectList(null)) {
            String key;
            if ("orderType".equals(field)) {
                key = order.getOrderType();
            } else if ("customerCode".equals(field)) {
                key = order.getCustomerCode();
            } else if ("carrierCode".equals(field)) {
                key = order.getCarrierCode();
            } else if ("serviceLevelCode".equals(field)) {
                key = order.getServiceLevelCode();
            } else {
                key = order.getRegionCode();
            }
            Map<String, Object> item = result.get(key);
            if (item == null) {
                item = new LinkedHashMap<>();
                item.put("name", key);
                item.put("count", 0);
                item.put("totalWeight", 0);
                item.put("totalVolume", 0);
                item.put("freightAmount", 0);
                result.put(key, item);
            }
            item.put("count", ((Integer) item.get("count")) + 1);
        }
        return new ArrayList<>(result.values());
    }

    private Object transitHours(Waybill waybill) {
        if (waybill.getActualDepartTime() == null || waybill.getActualArriveTime() == null) {
            return 0;
        }
        return java.time.Duration.between(waybill.getActualDepartTime(), waybill.getActualArriveTime()).toMinutes() / 60.0;
    }
}
