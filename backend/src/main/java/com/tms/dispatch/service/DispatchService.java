package com.tms.dispatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.basic.entity.Carrier;
import com.tms.basic.entity.Driver;
import com.tms.basic.entity.Route;
import com.tms.basic.entity.Vehicle;
import com.tms.basic.mapper.CarrierMapper;
import com.tms.basic.mapper.DriverMapper;
import com.tms.basic.mapper.RouteMapper;
import com.tms.basic.mapper.VehicleMapper;
import com.tms.billing.entity.FreightBill;
import com.tms.billing.service.BillingService;
import com.tms.billing.service.FareFormula;
import com.tms.billing.service.QuoteNote;
import com.tms.common.BizException;
import com.tms.common.CarrierRates;
import com.tms.common.CodeGenerator;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.integration.client.OmsSignClient;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.order.service.VolumeService;
import com.tms.thirdparty.ThirdPartyLogisticsAdapter;
import com.tms.thirdparty.ThirdPartyLogisticsGateway;
import com.tms.tracking.entity.TrackingEvent;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final RouteMapper routeMapper;
    private final TrackingEventMapper eventMapper;
    private final CodeGenerator codeGenerator;
    private final VolumeService volumeService;
    private final BillingService billingService;
    private final ThirdPartyLogisticsGateway gateway;

    @Autowired(required = false)
    private OmsSignClient omsSignClient;

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
        if ("DISPATCHED".equals(w.getStatus())
                || "IN_TRANSIT".equals(w.getStatus())
                || "ARRIVED".equals(w.getStatus())) {
            return load(id);
        }
        if (!"CREATED".equals(w.getStatus())) {
            throw new BizException("仅CREATED运单可dispatch");
        }
        if ("THIRD_PARTY".equals(w.getCarrierType())) {
            ThirdPartyLogisticsGateway.ShipmentResult r = gateway.create(w, orders(w));
            w.setThirdPartyNo(r.getThirdPartyNo());
            w.setThirdPartyStatus("ACCEPTED");
        }
        w.setStatus("DISPATCHED");
        Route route =
                w.getRouteCode() == null
                        ? null
                        : routeMapper.selectOne(
                                new LambdaQueryWrapper<Route>().eq(Route::getCode, w.getRouteCode()));
        BigDecimal distance = route == null ? BigDecimal.ZERO : route.getDistanceKm();
        for (TransportOrder o : orders(w)) {
            billingService.createBill(w, o, distance);
        }
        String vehicleType = vehicleType(w.getVehiclePlate());
        BigDecimal freight = BigDecimal.ZERO;
        for (FreightBill bill : billingService.bills(w.getId())) {
            bill.setAmount(FareFormula.total(bill.getAmount(), distance, vehicleType));
            bill.setCalcDetail(
                    QuoteNote.explain(
                            distance,
                            vehicleType,
                            w.getVehiclePlate(),
                            FareFormula.detail(bill.getCalcDetail(), distance, vehicleType)));
            billingService.updateBill(bill);
            freight = freight.add(bill.getAmount());
        }
        w.setFreightAmount(freight);
        waybillMapper.updateById(w);
        event(w, null, "DISPATCHED", null, null, "已调度");
        return load(id);
    }

    private String vehicleType(String plateNo) {
        if (plateNo == null || plateNo.trim().isEmpty()) {
            return null;
        }
        Vehicle vehicle =
                vehicleMapper.selectOne(
                        new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getPlateNo, plateNo.trim()));
        return vehicle == null ? null : vehicle.getVehicleType();
    }

    @Transactional
    public Waybill depart(Long id) {
        Waybill w = require(id);
        if (!"DISPATCHED".equals(w.getStatus())) {
            throw new BizException("仅DISPATCHED运单可发车");
        }
        w.setStatus("IN_TRANSIT");
        w.setActualDepartTime(LocalDateTime.now());
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
        waybillMapper.updateById(w);
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
        if (omsSignClient != null && "DELIVERED".equals(o.getStatus()) && !"RETURN".equals(o.getOrderType())) {
            omsSignClient.push(o);
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

    public Waybill requireByCode(String code) {
        Waybill w = findWaybill(code);
        if (w == null) {
            throw new BizException("运单不存在: " + code);
        }
        return w;
    }

    @Transactional
    public Waybill dispatchByCode(String code) {
        return dispatchByCode(code, null);
    }

    @Transactional
    public Waybill dispatchByCode(String code, String carrierCode) {
        Waybill w = findWaybill(code);
        if (w == null) {
            TransportOrder order = findOrder(code);
            if (order != null && notBlank(order.getWaybillCode())) {
                w = findWaybill(order.getWaybillCode());
            } else if (order != null && "CREATED".equals(order.getStatus())) {
                CreateReq req = new CreateReq();
                req.setCarrierCode(notBlank(carrierCode) ? carrierCode.trim() : "SF");
                req.setOrderIds(Collections.singletonList(order.getId()));
                req.setFromSiteCode(order.getFromSiteCode());
                req.setPlannedDepartTime(LocalDateTime.now());
                req.setPlannedArriveTime(LocalDateTime.now().plusHours(6));
                w = create(req);
            }
        }
        if (w == null) {
            throw new BizException("运单不存在: " + code);
        }
        if ("CREATED".equals(w.getStatus())) {
            return dispatch(w.getId());
        }
        if ("DISPATCHED".equals(w.getStatus())
                || "IN_TRANSIT".equals(w.getStatus())
                || "ARRIVED".equals(w.getStatus())) {
            return load(w.getId());
        }
        throw new BizException("当前状态不可调度: " + w.getStatus());
    }

    private Waybill findWaybill(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BizException("运单号必填");
        }
        return waybillMapper.selectOne(
                new LambdaQueryWrapper<Waybill>().eq(Waybill::getCode, code.trim()));
    }

    private TransportOrder findOrder(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        String value = code.trim();
        TransportOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<TransportOrder>().eq(TransportOrder::getCode, value));
        if (order != null) {
            return order;
        }
        return orderMapper.selectOne(
                new LambdaQueryWrapper<TransportOrder>().eq(TransportOrder::getSourceNo, value)
                        .last("LIMIT 1"));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Transactional
    public Waybill syncTrackByCode(String code) {
        Waybill w = requireByCode(code);
        if (w.getThirdPartyNo() != null && !w.getThirdPartyNo().trim().isEmpty()) {
            return syncTrack(w.getId());
        }
        applyIrTrack(w);
        event(w, null, "IR_SYNC", null, null, "IR控制塔同步轨迹");
        return load(w.getId());
    }

    private void applyIrTrack(Waybill w) {
        if (terminalStatus(w.getStatus())) {
            return;
        }
        Long id = w.getId();
        if ("CREATED".equals(w.getStatus())) {
            dispatch(id);
            depart(id);
            w = require(id);
        } else if ("DISPATCHED".equals(w.getStatus())) {
            depart(id);
            w = require(id);
        }
        LocalDateTime now = LocalDateTime.now();
        w.setExceptionFlag(false);
        if (w.getPlannedArriveTime() == null || !w.getPlannedArriveTime().isAfter(now)) {
            w.setPlannedArriveTime(now.plusHours(6));
        }
        waybillMapper.updateById(w);
    }

    private boolean terminalStatus(String status) {
        return "DELIVERED".equals(status)
                || "CLOSED".equals(status)
                || "CANCELLED".equals(status);
    }

    @Transactional
    public Waybill switchCarrierByCode(String code, String carrierCode) {
        if (carrierCode == null || carrierCode.trim().isEmpty()) {
            throw new BizException("承运商编码必填");
        }
        Waybill w = requireByCode(code);
        if ("DELIVERED".equals(w.getStatus())
                || "CLOSED".equals(w.getStatus())
                || "CANCELLED".equals(w.getStatus())) {
            throw new BizException("当前状态不可换承运商: " + w.getStatus());
        }
        Carrier c = carrierMapper.selectOne(
                new LambdaQueryWrapper<Carrier>().eq(Carrier::getCode, carrierCode.trim()));
        if (c == null) {
            throw new BizException("承运商不存在: " + carrierCode);
        }
        String fromCarrier = w.getCarrierCode();
        BigDecimal fromFreight = w.getFreightAmount();
        BigDecimal toFreight = CarrierRates.scaledFreight(fromCarrier, c.getCode(), fromFreight);
        w.setCarrierCode(c.getCode());
        w.setCarrierType(c.getType());
        if (toFreight != null) {
            w.setFreightAmount(toFreight);
        }
        waybillMapper.updateById(w);
        for (FreightBill bill : billingService.bills(w.getId())) {
            if ("FREIGHT_DELTA".equals(bill.getChargeType())) {
                continue;
            }
            bill.setCarrierCode(c.getCode());
            bill.setAmount(CarrierRates.scaledFreight(fromCarrier, c.getCode(), bill.getAmount()));
            billingService.updateBill(bill);
        }
        billingService.recordFreightDelta(w, fromCarrier, fromFreight, toFreight);
        event(w, null, "SWITCH_CARRIER", null, null, "IR 换承运商 " + carrierCode.trim());
        return load(w.getId());
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
}
