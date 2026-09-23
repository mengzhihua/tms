package com.tms.dispatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tms.basic.entity.Site;
import com.tms.basic.entity.Vehicle;
import com.tms.basic.mapper.SiteMapper;
import com.tms.basic.mapper.VehicleMapper;
import com.tms.dispatch.service.MapBinder.Assignment;
import com.tms.dispatch.service.MapBinder.Stop;
import com.tms.dispatch.service.MapBinder.Truck;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 待派订单按收货坐标，没有收货坐标时用起点站，绑到最近的空闲车。 */
@Service
@RequiredArgsConstructor
public class MapBindService {
    private final TransportOrderMapper orderMapper;
    private final VehicleMapper vehicleMapper;
    private final SiteMapper siteMapper;

    public List<Assignment> bind(String fromSiteCode) {
        QueryWrapper<TransportOrder> query = new QueryWrapper<TransportOrder>().eq("status", "CREATED");
        if (fromSiteCode != null && !fromSiteCode.trim().isEmpty()) {
            query.eq("from_site_code", fromSiteCode.trim());
        }
        Site site = site(fromSiteCode);
        List<Stop> stops = new ArrayList<Stop>();
        for (TransportOrder order : orderMapper.selectList(query)) {
            BigDecimal lng = order.getConsigneeLng();
            BigDecimal lat = order.getConsigneeLat();
            if ((lng == null || lat == null) && site != null) {
                lng = site.getLng();
                lat = site.getLat();
            }
            stops.add(new Stop(
                    order.getCode(),
                    lng,
                    lat,
                    order.getTotalWeightKg(),
                    order.getTotalVolumeM3(),
                    order.getPriority() == null ? 0 : order.getPriority()));
        }
        List<Truck> trucks = new ArrayList<Truck>();
        for (Vehicle vehicle : vehicleMapper.selectList(new LambdaQueryWrapper<Vehicle>().eq(Vehicle::getStatus, "IDLE"))) {
            trucks.add(new Truck(
                    vehicle.getId(),
                    vehicle.getPlateNo(),
                    vehicle.getVehicleType(),
                    vehicle.getLng(),
                    vehicle.getLat(),
                    vehicle.getMaxWeightKg(),
                    vehicle.getMaxVolumeM3()));
        }
        return MapBinder.bind(stops, trucks);
    }

    private Site site(String fromSiteCode) {
        if (fromSiteCode == null || fromSiteCode.trim().isEmpty()) {
            return null;
        }
        return siteMapper.selectOne(new LambdaQueryWrapper<Site>().eq(Site::getCode, fromSiteCode.trim()));
    }
}
