package com.tms.tracking.controller;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper; import com.baomidou.mybatisplus.extension.plugins.pagination.Page; import com.tms.common.R; import com.tms.tracking.entity.*; import com.tms.tracking.mapper.*; import com.tms.tracking.service.TrackingService; import com.tms.tracking.service.TrackingService.GpsReq; import com.tms.tracking.service.TrackingService.HandleReq; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/tracking") @RequiredArgsConstructor
public class TrackingController {
 private final TrackingService service; private final TrackingEventMapper eventMapper; private final GeofenceAlertMapper alertMapper;
 @PostMapping("/gps") public R<Integer> gps(@RequestBody GpsReq r){return R.ok(service.gps(r));}
 @GetMapping("/vehicles") public R<List<Map<String,Object>>> vehicles(){return R.ok(service.vehicles());}
 @GetMapping("/events/page") public R<Page<TrackingEvent>> events(@RequestParam(defaultValue="1")long current,@RequestParam(defaultValue="20")long size,@RequestParam(required=false)String waybillCode,@RequestParam(required=false)String eventType){QueryWrapper<TrackingEvent>q=new QueryWrapper<>();if(waybillCode!=null)q.eq("waybill_code",waybillCode);if(eventType!=null)q.eq("event_type",eventType);q.orderByDesc("event_time");return R.ok(eventMapper.selectPage(new Page<>(current,size),q));}
 @GetMapping("/alerts/page") public R<Page<GeofenceAlert>> alerts(@RequestParam(defaultValue="1")long current,@RequestParam(defaultValue="20")long size,@RequestParam(required=false)Boolean handled){QueryWrapper<GeofenceAlert>q=new QueryWrapper<>();if(handled!=null)q.eq("handled",handled);q.orderByDesc("alert_time");return R.ok(alertMapper.selectPage(new Page<>(current,size),q));}
 @PostMapping("/alerts/{id}/handle") public R<Void> handle(@PathVariable Long id,@RequestBody HandleReq r){service.handle(id,r);return R.ok();}
 @PostMapping("/simulate/{waybillId}") public R<Integer> simulate(@PathVariable Long waybillId,@RequestParam(defaultValue="10")int steps){return R.ok(service.simulate(waybillId,steps));}
}
