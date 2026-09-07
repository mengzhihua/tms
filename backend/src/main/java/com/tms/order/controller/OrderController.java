package com.tms.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tms.basic.entity.Vehicle;
import com.tms.basic.mapper.VehicleMapper;
import com.tms.common.BizException;
import com.tms.common.R;
import com.tms.order.entity.TransportOrder;
import com.tms.order.entity.TransportOrderLine;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.order.service.TransportOrderService;
import com.tms.order.service.VolumeService;
import java.math.BigDecimal;
import java.util.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final TransportOrderMapper mapper;
    private final TransportOrderService service;
    private final VehicleMapper vehicleMapper;

    @GetMapping("/page")
    public R<Page<TransportOrder>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<TransportOrder> q = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            q.and(
                    w ->
                            w.like("code", keyword)
                                    .or()
                                    .like("source_no", keyword)
                                    .or()
                                    .like("consignee_name", keyword));
        }
        q.orderByDesc("id");
        return R.ok(mapper.selectPage(new Page<>(current, size), q));
    }

    @GetMapping("/{id}")
    public R<TransportOrder> get(@PathVariable Long id) {
        return R.ok(service.load(id));
    }

    @PostMapping
    public R<TransportOrder> create(@RequestBody TransportOrder o) {
        return R.ok(service.create(o));
    }

    @PutMapping("/{id}")
    public R<TransportOrder> update(@PathVariable Long id, @RequestBody TransportOrder o) {
        return R.ok(service.update(id, o));
    }

    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return R.ok();
    }

    @PostMapping("/volume/calc")
    public R<VolumeService.VolumeResult> calc(@RequestBody CalcReq req) {
        return R.ok(VolumeService.calc(req.lines, req.volumeRatio));
    }

    @PostMapping("/volume/load-check")
    public R<VolumeService.LoadResult> loadCheck(@RequestBody LoadReq req) {
        Vehicle v = vehicleMapper.selectById(req.vehicleId);
        if (v == null) throw new BizException("车辆不存在");
        List<TransportOrder> os = mapper.selectBatchIds(req.orderIds);
        BigDecimal weight = BigDecimal.ZERO, volume = BigDecimal.ZERO;
        for (TransportOrder o : os) {
            weight = weight.add(o.getTotalWeightKg() == null ? BigDecimal.ZERO : o.getTotalWeightKg());
            volume = volume.add(o.getTotalVolumeM3() == null ? BigDecimal.ZERO : o.getTotalVolumeM3());
        }
        return R.ok(VolumeService.checkLoad(v, weight, volume));
    }

    @Data
    public static class CalcReq {
        private List<TransportOrderLine> lines;
        private BigDecimal volumeRatio;
    }

    @Data
    public static class LoadReq {
        private Long vehicleId;
        private List<Long> orderIds;
    }
}
