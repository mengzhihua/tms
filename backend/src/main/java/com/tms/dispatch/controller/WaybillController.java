package com.tms.dispatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tms.common.R;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.dispatch.service.DispatchService;
import com.tms.dispatch.service.DispatchService.CreateReq;
import com.tms.dispatch.service.DispatchService.SignReq;
import com.tms.order.entity.TransportOrder;
import com.tms.tracking.entity.TrackingEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/waybill")
@RequiredArgsConstructor
public class WaybillController {
    private final WaybillMapper mapper;
    private final DispatchService service;

    @GetMapping("/page")
    public R<Page<Waybill>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<Waybill> q = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            q.and(
                    w ->
                            w.like("code", keyword)
                                    .or()
                                    .like("vehicle_plate", keyword)
                                    .or()
                                    .like("third_party_no", keyword));
        }
        q.orderByDesc("id");
        return R.ok(mapper.selectPage(new Page<>(current, size), q));
    }

    @GetMapping("/{id}")
    public R<Waybill> get(@PathVariable Long id) {
        return R.ok(service.load(id));
    }

    @PostMapping
    public R<Waybill> create(@RequestBody CreateReq r) {
        return R.ok(service.create(r));
    }

    @PostMapping("/{id}/dispatch")
    public R<Waybill> dispatch(@PathVariable Long id) {
        return R.ok(service.dispatch(id));
    }

    @PostMapping("/{id}/depart")
    public R<Waybill> depart(@PathVariable Long id) {
        return R.ok(service.depart(id));
    }

    @PostMapping("/{id}/arrive")
    public R<Waybill> arrive(@PathVariable Long id) {
        return R.ok(service.arrive(id));
    }

    @PostMapping("/{id}/sign")
    public R<Waybill> sign(@PathVariable Long id, @RequestBody SignReq r) {
        return R.ok(service.sign(id, r));
    }

    @PostMapping("/{id}/close")
    public R<Waybill> close(@PathVariable Long id) {
        return R.ok(service.close(id));
    }

    @PostMapping("/{id}/cancel")
    public R<Waybill> cancel(@PathVariable Long id) {
        return R.ok(service.cancel(id));
    }

    @GetMapping("/{id}/orders")
    public R<List<TransportOrder>> orders(@PathVariable Long id) {
        return R.ok(service.orders(service.require(id)));
    }

    @GetMapping("/{id}/events")
    public R<List<TrackingEvent>> events(@PathVariable Long id) {
        return R.ok(service.load(id).getEvents());
    }

    @PostMapping("/{id}/sync-track")
    public R<Waybill> sync(@PathVariable Long id) {
        return R.ok(service.syncTrack(id));
    }
}
