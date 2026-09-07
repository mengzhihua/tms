package com.tms.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.tracking.entity.GeofenceAlert;
import com.tms.tracking.entity.TrackingEvent;
import com.tms.tracking.mapper.GeofenceAlertMapper;
import com.tms.tracking.mapper.TrackingEventMapper;
import com.tms.basic.entity.Vehicle;
import com.tms.basic.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final TransportOrderMapper orderMapper;
    private final WaybillMapper waybillMapper;
    private final VehicleMapper vehicleMapper;
    private final GeofenceAlertMapper alertMapper;
    private final TrackingEventMapper eventMapper;
    @GetMapping
    public R<Map<String,Object>> summary() {
        Map<String,Object> m = new LinkedHashMap<>();
        Map<String,Long> os = new LinkedHashMap<>(), ws = new LinkedHashMap<>();
        for (TransportOrder o : orderMapper.selectList(null)) os.put(o.getStatus(), os.getOrDefault(o.getStatus(),0L)+1);
        for (Waybill w : waybillMapper.selectList(null)) ws.put(w.getStatus(), ws.getOrDefault(w.getStatus(),0L)+1);
        m.put("orderStatusCounts", os); m.put("waybillStatusCounts", ws);
        m.put("inTransitVehicles", vehicleMapper.selectCount(new QueryWrapper<Vehicle>().eq("status","IN_TRANSIT")));
        m.put("idleVehicles", vehicleMapper.selectCount(new QueryWrapper<Vehicle>().eq("status","IDLE")));
        m.put("todayWaybills", waybillMapper.selectCount(new QueryWrapper<Waybill>().ge("created_at", LocalDate.now().atStartOfDay())));
        m.put("unhandledAlerts", alertMapper.selectCount(new QueryWrapper<GeofenceAlert>().eq("handled", false)));
        BigDecimal freight = waybillMapper.selectList(null).stream().map(Waybill::getFreightAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("monthFreightAmount", freight);
        m.put("pendingOrdersTop10", orderMapper.selectList(new QueryWrapper<TransportOrder>().eq("status","CREATED").orderByDesc("priority").last("LIMIT 10")));
        m.put("recentEvents", eventMapper.selectList(new QueryWrapper<TrackingEvent>().orderByDesc("event_time").last("LIMIT 20")));
        return R.ok(m);
    }
}
