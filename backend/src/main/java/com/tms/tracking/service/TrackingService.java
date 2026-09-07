package com.tms.tracking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.basic.entity.Vehicle;
import com.tms.basic.mapper.VehicleMapper;
import com.tms.common.BizException;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.tracking.entity.*;
import com.tms.tracking.mapper.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackingService {
    private final VehicleMapper vehicleMapper;
    private final WaybillMapper waybillMapper;
    private final TransportOrderMapper orderMapper;
    private final TrackingEventMapper eventMapper;
    private final GeofenceService geofenceService;
    private final GeofenceAlertMapper alertMapper;

    public int gps(GpsReq r) {
        Vehicle v =
                r.vehiclePlate == null
                        ? null
                        : vehicleMapper.selectOne(
                                new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getPlateNo, r.vehiclePlate));
        Waybill w = r.waybillId == null ? null : waybillMapper.selectById(r.waybillId);
        if (w == null && v != null) {
            w =
                    waybillMapper.selectOne(
                            new LambdaQueryWrapper<Waybill>()
                                    .eq(Waybill::getVehiclePlate, v.getPlateNo())
                                    .eq(Waybill::getStatus, "IN_TRANSIT"));
        }
        if (v != null) {
            v.setLng(r.lng);
            v.setLat(r.lat);
            v.setLastGpsTime(r.eventTime == null ? LocalDateTime.now() : r.eventTime);
            vehicleMapper.updateById(v);
        }
        if (w == null) return 0;
        TrackingEvent e = new TrackingEvent();
        e.setWaybillId(w.getId());
        e.setWaybillCode(w.getCode());
        e.setEventType("GPS");
        e.setLng(r.lng);
        e.setLat(r.lat);
        e.setSpeedKmh(r.speedKmh);
        e.setAddress(r.address);
        e.setEventTime(r.eventTime == null ? LocalDateTime.now() : r.eventTime);
        e.setSource("GPS");
        eventMapper.insert(e);
        return geofenceService.onPosition(w, r.lng, r.lat).size();
    }

    public int simulate(Long waybillId, int steps) {
        Waybill w = waybillMapper.selectById(waybillId);
        if (w == null) throw new BizException("运单不存在");
        Vehicle v =
                vehicleMapper.selectOne(
                        new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getPlateNo, w.getVehiclePlate()));
        List<TransportOrder> orders =
                orderMapper.selectList(
                        new LambdaQueryWrapper<TransportOrder>().eq(TransportOrder::getWaybillId, waybillId));
        if (v == null || orders.isEmpty()) return 0;
        TransportOrder o = orders.get(0);
        BigDecimal aLng = v.getLng() == null ? new BigDecimal("121.47") : v.getLng(),
                aLat = v.getLat() == null ? new BigDecimal("31.23") : v.getLat(),
                bLng = o.getConsigneeLng() == null ? aLng : o.getConsigneeLng(),
                bLat = o.getConsigneeLat() == null ? aLat : o.getConsigneeLat();
        int total = 0;
        for (int i = 0; i < Math.max(2, steps); i++) {
            BigDecimal f =
                    new BigDecimal(i)
                            .divide(new BigDecimal(Math.max(1, steps - 1)), 8, java.math.RoundingMode.HALF_UP);
            GpsReq r = new GpsReq();
            r.setVehiclePlate(w.getVehiclePlate());
            r.setLng(aLng.add(bLng.subtract(aLng).multiply(f)));
            r.setLat(aLat.add(bLat.subtract(aLat).multiply(f)));
            r.setSpeedKmh(new BigDecimal("60"));
            total += gps(r);
        }
        return total;
    }

    public List<Map<String, Object>> vehicles() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Vehicle v : vehicleMapper.selectList(null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vehicle", v);
            Waybill w =
                    waybillMapper.selectOne(
                            new LambdaQueryWrapper<Waybill>()
                                    .eq(Waybill::getVehiclePlate, v.getPlateNo())
                                    .eq(Waybill::getStatus, "IN_TRANSIT"));
            m.put("waybill", w);
            out.add(m);
        }
        return out;
    }

    public void handle(Long id, HandleReq r) {
        GeofenceAlert a = alertMapper.selectById(id);
        if (a == null) throw new BizException("告警不存在");
        a.setHandled(true);
        a.setHandler(r.handler);
        a.setHandleRemark(r.handleRemark);
        alertMapper.updateById(a);
    }

    @Data
    public static class GpsReq {
        private String vehiclePlate;
        private Long waybillId;
        private BigDecimal lng, lat, speedKmh;
        private String address;
        private LocalDateTime eventTime;
    }

    @Data
    public static class HandleReq {
        private String handler, handleRemark;
    }
}
