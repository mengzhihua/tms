package com.tms.dispatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.basic.entity.Carrier;
import com.tms.basic.entity.CarrierCoverage;
import com.tms.basic.entity.Driver;
import com.tms.basic.entity.Route;
import com.tms.basic.mapper.CarrierCoverageMapper;
import com.tms.basic.entity.Vehicle;
import com.tms.basic.mapper.CarrierMapper;
import com.tms.basic.mapper.DriverMapper;
import com.tms.basic.mapper.RouteMapper;
import com.tms.basic.mapper.VehicleMapper;
import com.tms.basic.entity.ServiceLevel;
import com.tms.basic.mapper.ServiceLevelMapper;
import com.tms.billing.entity.FreightBill;
import com.tms.billing.service.BillingService;
import com.tms.common.BizException;
import com.tms.common.CodeGenerator;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.order.service.VolumeService;
import com.tms.pod.service.PodService;
import com.tms.exc.entity.TransportException;
import com.tms.exc.service.ExceptionService;
import com.tms.openapi.RoutePushService;
import com.tms.thirdparty.ThirdPartyLogisticsAdapter;
import com.tms.thirdparty.ThirdPartyLogisticsGateway;
import com.tms.tracking.entity.TrackingEvent;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispatchService {
    private final WaybillMapper waybillMapper;
    private final TransportOrderMapper orderMapper;
    private final VehicleMapper vehicleMapper;
    private final DriverMapper driverMapper;
    private final CarrierMapper carrierMapper;
    private final CarrierCoverageMapper coverageMapper;
    private final RouteMapper routeMapper;
    private final TrackingEventMapper eventMapper;
    private final CodeGenerator codeGenerator;
    private final VolumeService volumeService;
    private final BillingService billingService;
    private final ThirdPartyLogisticsGateway gateway;
    private final ServiceLevelMapper serviceLevelMapper;
    private final PodService podService;
    private final ExceptionService exceptionService;
    private final RoutePushService routePushService;

    @Transactional
    public Waybill create(CreateReq req) {
        Carrier c =
                carrierMapper.selectOne(
                        new LambdaQueryWrapper<Carrier>().eq(Carrier::getCode, req.carrierCode));
        if (c == null) {
            throw new BizException("承运商不存在");
        }
        List<TransportOrder> orders = new ArrayList<>();
        for (Long id : req.orderIds) {
            TransportOrder o = orderMapper.selectById(id);
            if (o == null || !"CREATED".equals(o.getStatus())) {
                throw new BizException("订单必须为CREATED");
            }
            if (o.getCarrierCode() != null && !o.getCarrierCode().equals(req.carrierCode)) {
                throw new BizException("订单 " + o.getCode() + " 已分配承运商 " + o.getCarrierCode());
            }
            orders.add(o);
        }
        Vehicle v = null;
        if ("SELF".equals(c.getType())) {
            if (req.vehicleId == null || req.driverCode == null) {
                throw new BizException("自建承运商必须选择车辆和司机");
            }
            v = vehicleMapper.selectById(req.vehicleId);
            if (v == null || !"IDLE".equals(v.getStatus())) {
                throw new BizException("车辆不可用");
            }
            VolumeService.LoadResult lr =
                    volumeService.checkLoad(v, sumWeight(orders), sumVolume(orders));
            if (!lr.isFits()) {
                throw new BizException("超载");
            }
        }
        Waybill w = new Waybill();
        w.setCode(codeGenerator.next("WB"));
        w.setCarrierCode(c.getCode());
        w.setCarrierType(c.getType());
        w.setVehiclePlate(v == null ? null : v.getPlateNo());
        w.setDriverCode(req.driverCode);
        Driver d =
                req.driverCode == null
                        ? null
                        : driverMapper.selectOne(
                                new LambdaQueryWrapper<Driver>().eq(Driver::getCode, req.driverCode));
        w.setDriverName(d == null ? null : d.getName());
        w.setRouteCode(req.routeCode);
        w.setFromSiteCode(req.fromSiteCode);
        if (!orders.isEmpty()) {
            w.setServiceLevelCode(orders.get(0).getServiceLevelCode());
        }
        w.setLoadStatus("NONE");
        w.setPlannedDepartTime(req.plannedDepartTime);
        w.setPlannedArriveTime(req.plannedArriveTime);
        w.setOrderCount(orders.size());
        w.setTotalWeightKg(sumWeight(orders));
        w.setTotalVolumeM3(sumVolume(orders));
        w.setWeightLoadRate(
                v == null
                        ? BigDecimal.ZERO
                        : volumeService
                                .checkLoad(v, w.getTotalWeightKg(), w.getTotalVolumeM3())
                                .getWeightRate());
        w.setVolumeLoadRate(
                v == null
                        ? BigDecimal.ZERO
                        : volumeService
                                .checkLoad(v, w.getTotalWeightKg(), w.getTotalVolumeM3())
                                .getVolumeRate());
        w.setFreightAmount(BigDecimal.ZERO);
        w.setStatus("CREATED");
        w.setExceptionFlag(false);
        waybillMapper.insert(w);
        for (TransportOrder o : orders) {
            o.setStatus("DISPATCHED");
            o.setWaybillId(w.getId());
            o.setWaybillCode(w.getCode());
            orderMapper.updateById(o);
            event(w, o.getId(), "CREATED", null, null, "订单创建运单");
        }
        event(w, null, "CREATED", null, null, "运单创建");
        return load(w.getId());
    }

    @Transactional
    public Waybill dispatch(Long id) {
        Waybill w = require(id);
        if (!"CREATED".equals(w.getStatus())) {
            throw new BizException("仅CREATED运单可dispatch");
        }
        if ("THIRD_PARTY".equals(w.getCarrierType())) {
            ThirdPartyLogisticsGateway.ShipmentResult r = gateway.create(w, orders(w));
            w.setThirdPartyNo(r.getThirdPartyNo());
            w.setThirdPartyStatus("ACCEPTED");
        }
        w.setStatus("DISPATCHED");
        BigDecimal freight = BigDecimal.ZERO;
        Route route =
                w.getRouteCode() == null
                        ? null
                        : routeMapper.selectOne(
                                new LambdaQueryWrapper<Route>().eq(Route::getCode, w.getRouteCode()));
        BigDecimal distance = route == null ? BigDecimal.ZERO : route.getDistanceKm();
        for (TransportOrder o : orders(w)) {
            routePushService.push(
                    o.getCustomerCode(),
                    o.getCode(),
                    o.getSourceNo(),
                    "DISPATCHED",
                    "DISPATCHED",
                    "运单已调度",
                    w);
        }
        for (TransportOrder o : orders(w)) {
            freight = freight.add(billingService.createBill(w, o, distance));
        }
        w.setFreightAmount(freight);
        waybillMapper.updateById(w);
        event(w, null, "DISPATCHED", null, null, "已调度");
        return load(id);
    }

    @Transactional
    public Waybill depart(Long id) {
        Waybill w = require(id);
        if (!"DISPATCHED".equals(w.getStatus())) {
            throw new BizException("仅DISPATCHED运单可发车");
        }
        if ("SELF".equals(w.getCarrierType()) && !"LOADED".equals(w.getLoadStatus())) {
            throw new BizException("自建运单必须先装车交接");
        }
        w.setStatus("IN_TRANSIT");
        w.setActualDepartTime(LocalDateTime.now());
        BigDecimal promisedHours = coverageHours(w);
        if (promisedHours == null && w.getServiceLevelCode() != null) {
            ServiceLevel level =
                    serviceLevelMapper.selectOne(
                            new LambdaQueryWrapper<ServiceLevel>()
                                    .eq(ServiceLevel::getCode, w.getServiceLevelCode()));
            if (level != null) {
                promisedHours = level.getPromisedHours();
            }
        }
        if (promisedHours != null) {
            w.setPromisedArriveTime(
                    w.getActualDepartTime()
                            .plusMinutes(promisedHours.multiply(new BigDecimal("60")).longValue()));
        }
        waybillMapper.updateById(w);
        if (w.getVehiclePlate() != null) {
            Vehicle v =
                    vehicleMapper.selectOne(
                            new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getPlateNo, w.getVehiclePlate()));
            if (v != null) {
                v.setStatus("IN_TRANSIT");
                vehicleMapper.updateById(v);
            }
        }
        for (TransportOrder o : orders(w)) {
            o.setStatus("IN_TRANSIT");
            orderMapper.updateById(o);
            routePushService.push(
                    o.getCustomerCode(),
                    o.getCode(),
                    o.getSourceNo(),
                    "IN_TRANSIT",
                    "IN_TRANSIT",
                    "运单已发车",
                    w);
        }
        event(w, null, "DEPARTED", null, null, "已发车");
        return load(id);
    }

    @Transactional
    public Waybill arrive(Long id) {
        Waybill w = require(id);
        if (!"IN_TRANSIT".equals(w.getStatus())) {
            throw new BizException("仅IN_TRANSIT运单可到达");
        }
        w.setStatus("ARRIVED");
        w.setActualArriveTime(LocalDateTime.now());
        if (w.getPromisedArriveTime() != null) {
            w.setOnTime(!w.getActualArriveTime().isAfter(w.getPromisedArriveTime()));
        }
        waybillMapper.updateById(w);
        for (TransportOrder o : orders(w)) {
            routePushService.push(
                    o.getCustomerCode(),
                    o.getCode(),
                    o.getSourceNo(),
                    "ARRIVED",
                    "ARRIVED",
                    "运单已到达",
                    w);
        }
        event(w, null, "ARRIVED", null, null, "已到达");
        return load(id);
    }

    @Transactional
    public Waybill sign(Long id, SignReq req) {
        Waybill w = require(id);
        if (!"IN_TRANSIT".equals(w.getStatus()) && !"ARRIVED".equals(w.getStatus())) {
            throw new BizException("运单状态不可签收");
        }
        TransportOrder o = orderMapper.selectById(req.orderId);
        if (o == null || !id.equals(o.getWaybillId())) {
            throw new BizException("订单不属于该运单");
        }
        o.setSigner(req.signer);
        o.setSignTime(req.signTime == null ? LocalDateTime.now() : req.signTime);
        o.setPodImage(req.podImage);
        o.setPodRemark(req.remark);
        o.setStatus(Boolean.TRUE.equals(req.exception) ? "EXCEPTION" : "DELIVERED");
        orderMapper.updateById(o);
        routePushService.push(
                o.getCustomerCode(),
                o.getCode(),
                o.getSourceNo(),
                o.getStatus(),
                Boolean.TRUE.equals(req.exception) ? "EXCEPTION" : "DELIVERED",
                req.remark,
                w);
        podService.create(w, o, req.podImage);
        if (Boolean.TRUE.equals(req.exception)) {
            TransportException exception = new TransportException();
            exception.setWaybillId(w.getId());
            exception.setWaybillCode(w.getCode());
            exception.setOrderId(o.getId());
            exception.setOrderCode(o.getCode());
            exception.setCarrierCode(w.getCarrierCode());
            exception.setType("REJECT");
            exception.setLevel("MEDIUM");
            exception.setDescription(req.remark);
            exceptionService.create(exception);
        }
        w.setExceptionFlag(
                Boolean.TRUE.equals(req.exception)
                        || (w.getExceptionFlag() != null && w.getExceptionFlag()));
        if (Boolean.TRUE.equals(req.exception)) {
            w.setExceptionRemark(req.remark);
        }
        event(
                w,
                o.getId(),
                Boolean.TRUE.equals(req.exception) ? "EXCEPTION" : "SIGNED",
                null,
                null,
                req.remark);
        boolean done = true;
        for (TransportOrder x : orders(w)) {
            if (!"DELIVERED".equals(x.getStatus()) && !"EXCEPTION".equals(x.getStatus())) {
                done = false;
            }
        }
        if (done) {
            w.setStatus("DELIVERED");
            waybillMapper.updateById(w);
            if (w.getVehiclePlate() != null) {
                Vehicle v =
                        vehicleMapper.selectOne(
                                new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getPlateNo, w.getVehiclePlate()));
                if (v != null) {
                    v.setStatus("IDLE");
                    vehicleMapper.updateById(v);
                }
            }
        } else {
            waybillMapper.updateById(w);
        }
        return load(id);
    }

    @Transactional
    public Waybill close(Long id) {
        Waybill w = require(id);
        if (!"DELIVERED".equals(w.getStatus())) {
            throw new BizException("仅DELIVERED运单可关闭");
        }
        w.setStatus("CLOSED");
        waybillMapper.updateById(w);
        for (FreightBill b : billingService.bills(w.getId())) {
            if ("UNBILLED".equals(b.getStatus())) {
                b.setStatus("BILLED");
                billingService.updateBill(b);
            }
        }
        return load(id);
    }

    @Transactional
    public Waybill syncTrack(Long id) {
        Waybill w = require(id);
        if (w.getThirdPartyNo() == null) {
            throw new BizException("运单没有三方单号");
        }
        for (ThirdPartyLogisticsAdapter.TrackEvent t :
                gateway.query(w.getCarrierCode(), w.getThirdPartyNo())) {
            boolean exists =
                    !eventMapper
                            .selectList(
                                    new LambdaQueryWrapper<TrackingEvent>()
                                            .eq(TrackingEvent::getWaybillId, w.getId())
                                            .eq(TrackingEvent::getEventType, "THIRD_PARTY")
                                            .eq(TrackingEvent::getDescription, t.getDescription())
                                            .eq(TrackingEvent::getEventTime, t.getEventTime()))
                            .isEmpty();
            w = require(id);
            if (exists) {
                continue;
            }
            TrackingEvent e = new TrackingEvent();
            e.setWaybillId(w.getId());
            e.setWaybillCode(w.getCode());
            e.setEventType("THIRD_PARTY");
            e.setDescription(t.getDescription());
            e.setEventTime(t.getEventTime());
            e.setSource("THIRD_PARTY");
            eventMapper.insert(e);
            w.setThirdPartyStatus(t.getStatusCode());
            if ("IN_TRANSIT".equals(t.getStatusCode()) && "DISPATCHED".equals(w.getStatus())) {
                depart(w.getId());
                w = require(id);
            }
            if ("SIGNED".equals(t.getStatusCode())) {
                completeThirdParty(w.getId());
                w = require(id);
                w.setThirdPartyStatus(t.getStatusCode());
                waybillMapper.updateById(w);
            } else {
                waybillMapper.updateById(w);
            }
        }
        return load(id);
    }

    @Transactional
    public Waybill completeThirdParty(Long id) {
        Waybill w = require(id);
        if ("DISPATCHED".equals(w.getStatus())) {
            depart(id);
            w = require(id);
        }
        if ("IN_TRANSIT".equals(w.getStatus())) {
            arrive(id);
            w = require(id);
        }
        if (!"ARRIVED".equals(w.getStatus())) {
            return load(id);
        }
        for (TransportOrder order : orders(w)) {
            if (!"DELIVERED".equals(order.getStatus()) && !"EXCEPTION".equals(order.getStatus())) {
                SignReq signReq = new SignReq();
                signReq.setOrderId(order.getId());
                signReq.setSigner("三方回传");
                sign(id, signReq);
                w = require(id);
                if ("DELIVERED".equals(w.getStatus())) {
                    break;
                }
            }
        }
        return load(id);
    }

    @Transactional
    public Waybill cancel(Long id) {
        Waybill w = require(id);
        if (!"CREATED".equals(w.getStatus()) && !"DISPATCHED".equals(w.getStatus())) {
            throw new BizException("仅CREATED/DISPATCHED运单可取消");
        }
        if ("THIRD_PARTY".equals(w.getCarrierType()) && w.getThirdPartyNo() != null) {
            gateway.cancel(w.getCarrierCode(), w.getThirdPartyNo());
        }
        for (TransportOrder o : orders(w)) {
            routePushService.push(
                    o.getCustomerCode(),
                    o.getCode(),
                    o.getSourceNo(),
                    "CANCELLED",
                    "CANCELLED",
                    "运单已取消",
                    w);
            o.setStatus("CREATED");
            o.setWaybillId(null);
            o.setWaybillCode(null);
            orderMapper.updateById(o);
        }
        if (w.getVehiclePlate() != null) {
            Vehicle v =
                    vehicleMapper.selectOne(
                            new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getPlateNo, w.getVehiclePlate()));
            if (v != null) {
                v.setStatus("IDLE");
                vehicleMapper.updateById(v);
            }
        }
        w.setStatus("CANCELLED");
        waybillMapper.updateById(w);
        event(w, null, "CANCELLED", null, null, "已取消");
        return load(id);
    }

    @Transactional
    public Waybill loadVehicle(Long id, LoadReq req) {
        Waybill w = require(id);
        if (!"DISPATCHED".equals(w.getStatus())) {
            throw new BizException("仅DISPATCHED运单可装车");
        }
        Set<String> expected = new HashSet<>();
        for (TransportOrder order : orders(w)) {
            expected.add(order.getCode());
        }
        Set<String> actual = new HashSet<>(req.orderCodes == null ? Collections.emptyList() : req.orderCodes);
        for (String code : expected) {
            if (!actual.contains(code)) {
                throw new BizException("订单 " + code + " 未装车");
            }
        }
        for (String code : actual) {
            if (!expected.contains(code)) {
                throw new BizException("订单 " + code + " 不属于本运单");
            }
        }
        w.setLoadStatus("LOADED");
        w.setSealNo(req.sealNo);
        w.setLoaderName(req.loaderName);
        w.setLoadTime(LocalDateTime.now());
        waybillMapper.updateById(w);
        event(w, null, "LOADED", null, null, "装车交接完成");
        return load(id);
    }

    public Map<String, Object> loadingSheet(Long id) {
        Waybill w = load(id);
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal volume = BigDecimal.ZERO;
        for (TransportOrder order : w.getOrders()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", order.getCode());
            row.put("consignee", order.getConsigneeName());
            row.put("qty", order.getTotalQty());
            row.put("weight", order.getTotalWeightKg());
            row.put("volume", order.getTotalVolumeM3());
            rows.add(row);
            weight = weight.add(order.getTotalWeightKg() == null ? BigDecimal.ZERO : order.getTotalWeightKg());
            volume = volume.add(order.getTotalVolumeM3() == null ? BigDecimal.ZERO : order.getTotalVolumeM3());
        }
        result.put("waybill", w);
        result.put("orders", rows);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("weight", weight);
        totals.put("volume", volume);
        result.put("totals", totals);
        result.put("sealNo", w.getSealNo());
        return result;
    }

    public Waybill load(Long id) {
        Waybill w = require(id);
        w.setOrders(orders(w));
        w.setEvents(
                eventMapper.selectList(
                        new LambdaQueryWrapper<TrackingEvent>()
                                .eq(TrackingEvent::getWaybillId, id)
                                .orderByAsc(TrackingEvent::getEventTime)));
        return w;
    }

    public Waybill require(Long id) {
        Waybill w = waybillMapper.selectById(id);
        if (w == null) {
            throw new BizException("运单不存在");
        }
        return w;
    }

    public List<TransportOrder> orders(Waybill w) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<TransportOrder>().eq(TransportOrder::getWaybillId, w.getId()));
    }

    private BigDecimal sumWeight(List<TransportOrder> x) {
        return x.stream()
                .map(TransportOrder::getTotalWeightKg)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumVolume(List<TransportOrder> x) {
        return x.stream()
                .map(TransportOrder::getTotalVolumeM3)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal coverageHours(Waybill waybill) {
        List<TransportOrder> linked = orders(waybill);
        if (linked.isEmpty() || linked.get(0).getRegionCode() == null) {
            return null;
        }
        TransportOrder order = linked.get(0);
        List<CarrierCoverage> rows =
                coverageMapper.selectList(
                        new LambdaQueryWrapper<CarrierCoverage>()
                                .eq(CarrierCoverage::getCarrierCode, waybill.getCarrierCode())
                                .eq(CarrierCoverage::getRegionCode, order.getRegionCode())
                                .eq(CarrierCoverage::getStatus, "ENABLED"));
        CarrierCoverage fallback = null;
        for (CarrierCoverage row : rows) {
            if (row.getServiceLevelCode() == null) {
                fallback = row;
            }
            if (waybill.getServiceLevelCode() == null
                    || row.getServiceLevelCode() == null
                    || waybill.getServiceLevelCode().equals(row.getServiceLevelCode())) {
                if (row.getPromisedHours() != null) {
                    return row.getPromisedHours();
                }
            }
        }
        return fallback == null ? null : fallback.getPromisedHours();
    }

    private void event(
            Waybill w, Long oid, String type, BigDecimal lng, BigDecimal lat, String desc) {
        TrackingEvent e = new TrackingEvent();
        e.setWaybillId(w.getId());
        e.setWaybillCode(w.getCode());
        e.setOrderId(oid);
        e.setEventType(type);
        e.setLng(lng);
        e.setLat(lat);
        e.setDescription(desc);
        e.setEventTime(LocalDateTime.now());
        e.setSource("SYSTEM");
        eventMapper.insert(e);
    }

    @Data
    public static class CreateReq {
        private String carrierCode, driverCode, routeCode, fromSiteCode;
        private Long vehicleId;
        private LocalDateTime plannedDepartTime, plannedArriveTime;
        private List<Long> orderIds;
    }

    @Data
    public static class SignReq {
        private Long orderId;
        private String signer, podImage, remark;
        private LocalDateTime signTime;
        private Boolean exception;
    }

    @Data
    public static class LoadReq {
        private String sealNo;
        private String loaderName;
        private List<String> orderCodes;
    }
}
