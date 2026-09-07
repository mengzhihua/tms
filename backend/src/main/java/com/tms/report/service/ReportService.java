package com.tms.report.service;

import com.tms.billing.entity.FreightBill;
import com.tms.billing.mapper.FreightBillMapper;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.exc.entity.TransportException;
import com.tms.exc.mapper.TransportExceptionMapper;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.pod.entity.PodReceipt;
import com.tms.pod.mapper.PodReceiptMapper;
import com.tms.tracking.entity.GeofenceAlert;
import com.tms.tracking.mapper.GeofenceAlertMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final WaybillMapper waybillMapper;
    private final TransportOrderMapper orderMapper;
    private final TransportExceptionMapper exceptionMapper;
    private final GeofenceAlertMapper alertMapper;
    private final PodReceiptMapper podMapper;
    private final FreightBillMapper billMapper;

    public List<Map<String, Object>> sla(String from, String to, String carrierCode) {
        ReportWindow window = window(from, to);
        Map<String, List<Waybill>> grouped = new LinkedHashMap<>();
        for (Waybill waybill : waybills(window, carrierCode)) {
            String key = key(waybill.getCarrierCode(), waybill.getServiceLevelCode());
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(waybill);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (List<Waybill> rows : grouped.values()) {
            Waybill first = rows.get(0);
            int onTime = 0;
            BigDecimal transit = BigDecimal.ZERO;
            int transitCount = 0;
            for (Waybill row : rows) {
                if (Boolean.TRUE.equals(row.getOnTime())) {
                    onTime++;
                }
                if (row.getActualDepartTime() != null && row.getActualArriveTime() != null) {
                    transit =
                            transit.add(
                                    BigDecimal.valueOf(
                                            java.time.Duration.between(
                                                            row.getActualDepartTime(),
                                                            row.getActualArriveTime())
                                                    .toMinutes())
                                            .divide(new BigDecimal("60"), 3, RoundingMode.HALF_UP));
                    transitCount++;
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("carrier", first.getCarrierCode());
            item.put("serviceLevel", first.getServiceLevelCode());
            item.put("waybillCount", rows.size());
            item.put("onTimeCount", onTime);
            item.put("onTimeRate", rate(onTime, onTimeDenom(rows)));
            item.put("avgTransitHours", transitCount == 0
                    ? BigDecimal.ZERO
                    : transit.divide(BigDecimal.valueOf(transitCount), 3, RoundingMode.HALF_UP));
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> slaFlow(String from, String to, String carrierCode) {
        ReportWindow window = window(from, to);
        Map<String, Integer> grouped = new LinkedHashMap<>();
        for (TransportOrder order : orders(window, carrierCode)) {
            String key = String.valueOf(order.getFromSiteCode()) + " -> " + order.getConsigneeProvince();
            grouped.put(key, grouped.getOrDefault(key, 0) + 1);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split(" -> ", 2);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fromSiteCode", parts[0]);
            item.put("consigneeProvince", parts.length > 1 ? parts[1] : null);
            item.put("count", entry.getValue());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> transitSign(String from, String to, String carrierCode) {
        ReportWindow window = window(from, to);
        Map<LocalDate, Map<String, Object>> daily = new LinkedHashMap<>();
        for (Waybill waybill : waybills(window, carrierCode)) {
            LocalDate date =
                    waybill.getActualDepartTime() == null
                            ? window.from
                            : waybill.getActualDepartTime().toLocalDate();
            Map<String, Object> item = daily.computeIfAbsent(date, ignored -> dailyRow(date));
            increment(item, "departed");
            if (waybill.getActualArriveTime() != null) {
                increment(item, "arrived");
            }
            if ("DELIVERED".equals(waybill.getStatus()) || "CLOSED".equals(waybill.getStatus())) {
                increment(item, "signed");
            }
            if ("IN_TRANSIT".equals(waybill.getStatus())
                    && (waybill.getActualArriveTime() == null
                            || !waybill.getActualArriveTime().isBefore(window.to.atStartOfDay()))) {
                increment(item, "inTransitEnd");
            }
        }
        return new ArrayList<>(daily.values());
    }

    public List<Map<String, Object>> quality(String from, String to, String carrierCode) {
        ReportWindow window = window(from, to);
        List<Waybill> waybills = waybills(window, carrierCode);
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Waybill waybill : waybills) {
            Map<String, Object> item = grouped.computeIfAbsent(
                    waybill.getCarrierCode(), ignored -> qualityRow(waybill.getCarrierCode()));
            item.put("waybillCount", ((Integer) item.get("waybillCount")) + 1);
        }
        Map<Long, String> carrierByWaybill = new HashMap<>();
        Map<Long, Waybill> waybillById = new HashMap<>();
        for (Waybill waybill : waybills) {
            carrierByWaybill.put(waybill.getId(), waybill.getCarrierCode());
            waybillById.put(waybill.getId(), waybill);
        }
        for (TransportException exception : exceptionMapper.selectList(null)) {
            String carrier = exception.getCarrierCode();
            if (carrier == null && exception.getWaybillId() != null) {
                carrier = carrierByWaybill.get(exception.getWaybillId());
            }
            if (carrier == null || (carrierCode != null && !carrierCode.equals(carrier))) {
                continue;
            }
            Waybill exceptionWaybill =
                    exception.getWaybillId() == null
                            ? null
                            : waybillById.get(exception.getWaybillId());
            if (exception.getWaybillId() != null
                    && (exceptionWaybill == null
                            || !inWindow(exceptionWaybill.getActualDepartTime(), window))) {
                continue;
            }
            final String exceptionCarrier = carrier;
            Map<String, Object> item =
                    grouped.computeIfAbsent(exceptionCarrier, ignored -> qualityRow(exceptionCarrier));
            item.put("exceptionCount", ((Integer) item.get("exceptionCount")) + 1);
            Map<String, Integer> byType = (Map<String, Integer>) item.get("byType");
            byType.put(exception.getType(), byType.getOrDefault(exception.getType(), 0) + 1);
            if (exception.getClaimAmount() != null) {
                item.put(
                        "claimAmount",
                        ((BigDecimal) item.get("claimAmount")).add(exception.getClaimAmount()));
            }
        }
        for (PodReceipt receipt : podMapper.selectList(null)) {
            Map<String, Object> item = grouped.get(receipt.getCarrierCode());
            if (item == null
                    || (receipt.getWaybillId() != null
                            && (waybillById.get(receipt.getWaybillId()) == null
                                    || !inWindow(
                                            waybillById.get(receipt.getWaybillId()).getActualDepartTime(),
                                            window)))) {
                continue;
            }
            int total = (Integer) item.get("podCount");
            int returned = (Integer) item.get("podReturned");
            item.put("podCount", total + 1);
            if ("RETURNED".equals(receipt.getStatus()) || "ARCHIVED".equals(receipt.getStatus())) {
                item.put("podReturned", returned + 1);
            }
        }
        for (GeofenceAlert alert : alertMapper.selectList(null)) {
            String carrier = carrierByWaybill.get(alert.getWaybillId());
            Map<String, Object> item = grouped.get(carrier);
            Waybill alertWaybill = waybillById.get(alert.getWaybillId());
            if (item != null
                    && alertWaybill != null
                    && inWindow(alertWaybill.getActualDepartTime(), window)) {
                item.put("alertCount", ((Integer) item.get("alertCount")) + 1);
            }
        }
        for (Map<String, Object> item : grouped.values()) {
            int count = (Integer) item.get("waybillCount");
            int exceptions = (Integer) item.get("exceptionCount");
            int pods = (Integer) item.get("podCount");
            int returned = (Integer) item.get("podReturned");
            item.put("exceptionRate", rate(exceptions, count));
            item.put("podReturnRate", rate(returned, pods));
            item.remove("podCount");
            item.remove("podReturned");
        }
        return new ArrayList<>(grouped.values());
    }

    public Map<String, Object> orderStructure(String from, String to, String carrierCode) {
        ReportWindow window = window(from, to);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byType", groupOrders(orders(window, carrierCode), "orderType"));
        result.put("byCustomer", topTen(groupOrders(orders(window, carrierCode), "customerCode")));
        result.put("byCarrier", groupOrders(orders(window, carrierCode), "carrierCode"));
        result.put("byServiceLevel", groupOrders(orders(window, carrierCode), "serviceLevelCode"));
        result.put("byRegion", groupOrders(orders(window, carrierCode), "regionCode"));
        return result;
    }

    public Map<String, Object> alertSummary(String from, String to, String carrierCode) {
        ReportWindow window = window(from, to);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Integer> exceptions = new LinkedHashMap<>();
        for (TransportException exception : exceptionMapper.selectList(null)) {
            if (!"CLOSED".equals(exception.getStatus())
                    && (carrierCode == null || carrierCode.equals(exception.getCarrierCode()))
                    && exceptionInWindow(exception, window)) {
                String key = exception.getType() + ":" + exception.getLevel();
                exceptions.put(key, exceptions.getOrDefault(key, 0) + 1);
            }
        }
        result.put("openExceptions", exceptions);
        int alerts = 0;
        for (GeofenceAlert alert : alertMapper.selectList(null)) {
            if (!Boolean.TRUE.equals(alert.getHandled())
                    && alert.getAlertTime() != null
                    && !alert.getAlertTime().isBefore(window.from.atStartOfDay())
                    && alert.getAlertTime().isBefore(window.to.atStartOfDay())) {
                alerts++;
            }
        }
        result.put("unhandledGeofenceAlerts", alerts);
        List<Waybill> imminent = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Waybill waybill : waybills(window, carrierCode)) {
            if ("IN_TRANSIT".equals(waybill.getStatus())
                    && waybill.getPromisedArriveTime() != null
                    && !waybill.getPromisedArriveTime().isBefore(now)
                    && !waybill.getPromisedArriveTime().isAfter(now.plusHours(2))) {
                imminent.add(waybill);
            }
        }
        result.put("imminentWaybills", imminent);
        return result;
    }

    private List<Waybill> waybills(ReportWindow window, String carrierCode) {
        List<Waybill> rows = waybillMapper.selectList(null);
        return rows.stream()
                .filter(row -> carrierCode == null || carrierCode.equals(row.getCarrierCode()))
                .filter(row -> inWindow(row.getActualDepartTime(), window))
                .collect(Collectors.toList());
    }

    private List<TransportOrder> orders(ReportWindow window, String carrierCode) {
        Map<Long, Waybill> waybillMap = waybillMapper.selectList(null).stream()
                .collect(Collectors.toMap(Waybill::getId, row -> row, (left, right) -> left));
        return orderMapper.selectList(null).stream()
                .filter(order -> carrierCode == null
                        || (order.getWaybillId() != null
                                && waybillMap.containsKey(order.getWaybillId())
                                && carrierCode.equals(waybillMap.get(order.getWaybillId()).getCarrierCode())))
                .filter(order -> {
                    Waybill waybill = waybillMap.get(order.getWaybillId());
                    return waybill != null
                            ? inWindow(waybill.getActualDepartTime(), window)
                            : inWindow(order.getCreatedAt(), window);
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> groupOrders(List<TransportOrder> orders, String field) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        Map<Long, BigDecimal> freight = new HashMap<>();
        for (FreightBill bill : billMapper.selectList(null)) {
            if (bill.getOrderId() != null && bill.getAmount() != null) {
                freight.put(
                        bill.getOrderId(),
                        freight.getOrDefault(bill.getOrderId(), BigDecimal.ZERO).add(bill.getAmount()));
            }
        }
        for (TransportOrder order : orders) {
            String name = value(order, field);
            Map<String, Object> item = grouped.computeIfAbsent(name, ignored -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", name);
                row.put("count", 0);
                row.put("totalWeight", BigDecimal.ZERO);
                row.put("totalVolume", BigDecimal.ZERO);
                row.put("freightAmount", BigDecimal.ZERO);
                return row;
            });
            item.put("count", ((Integer) item.get("count")) + 1);
            item.put(
                    "totalWeight",
                    ((BigDecimal) item.get("totalWeight"))
                            .add(nullToZero(order.getTotalWeightKg())));
            item.put(
                    "totalVolume",
                    ((BigDecimal) item.get("totalVolume"))
                            .add(nullToZero(order.getTotalVolumeM3())));
            item.put(
                    "freightAmount",
                    ((BigDecimal) item.get("freightAmount"))
                            .add(freight.getOrDefault(order.getId(), BigDecimal.ZERO)));
        }
        return new ArrayList<>(grouped.values());
    }

    private List<Map<String, Object>> topTen(List<Map<String, Object>> rows) {
        rows.sort(
                Comparator.comparing(
                        row -> (Integer) row.get("count"),
                        Comparator.reverseOrder()));
        return rows.size() > 10 ? new ArrayList<>(rows.subList(0, 10)) : rows;
    }

    private String value(TransportOrder order, String field) {
        if ("orderType".equals(field)) {
            return order.getOrderType();
        }
        if ("customerCode".equals(field)) {
            return order.getCustomerCode();
        }
        if ("carrierCode".equals(field)) {
            return order.getCarrierCode();
        }
        if ("serviceLevelCode".equals(field)) {
            return order.getServiceLevelCode();
        }
        return order.getRegionCode();
    }

    private Map<String, Object> qualityRow(String carrier) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("carrier", carrier);
        item.put("waybillCount", 0);
        item.put("exceptionCount", 0);
        item.put("exceptionRate", BigDecimal.ZERO);
        item.put("byType", new LinkedHashMap<String, Integer>());
        item.put("claimAmount", BigDecimal.ZERO);
        item.put("podReturnRate", BigDecimal.ZERO);
        item.put("podCount", 0);
        item.put("podReturned", 0);
        item.put("alertCount", 0);
        return item;
    }

    private Map<String, Object> dailyRow(LocalDate date) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("date", date);
        item.put("departed", 0);
        item.put("arrived", 0);
        item.put("signed", 0);
        item.put("inTransitEnd", 0);
        return item;
    }

    private void increment(Map<String, Object> row, String key) {
        row.put(key, ((Integer) row.get(key)) + 1);
    }

    private int onTimeDenom(List<Waybill> rows) {
        int count = 0;
        for (Waybill row : rows) {
            if (row.getOnTime() != null) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal rate(int numerator, int denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 3, RoundingMode.HALF_UP);
    }

    private boolean inWindow(LocalDateTime value, ReportWindow window) {
        return value != null
                && !value.isBefore(window.from.atStartOfDay())
                && value.isBefore(window.to.atStartOfDay());
    }

    private boolean exceptionInWindow(TransportException exception, ReportWindow window) {
        if (exception.getWaybillId() == null) {
            return true;
        }
        Waybill waybill = waybillMapper.selectById(exception.getWaybillId());
        return waybill != null && inWindow(waybill.getActualDepartTime(), window);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String key(String carrier, String serviceLevel) {
        return String.valueOf(carrier) + "|" + serviceLevel;
    }

    private ReportWindow window(String from, String to) {
        LocalDate end =
                to == null || to.isEmpty()
                        ? LocalDate.now().plusDays(1)
                        : LocalDate.parse(to).plusDays(1);
        LocalDate start = from == null || from.isEmpty() ? end.minusDays(30) : LocalDate.parse(from);
        return new ReportWindow(start, end);
    }

    private static class ReportWindow {
        private final LocalDate from;
        private final LocalDate to;

        private ReportWindow(LocalDate from, LocalDate to) {
            this.from = from;
            this.to = to;
        }
    }
}
