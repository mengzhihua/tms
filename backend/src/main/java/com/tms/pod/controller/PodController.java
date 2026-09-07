package com.tms.pod.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tms.common.R;
import com.tms.pod.entity.PodReceipt;
import com.tms.pod.mapper.PodReceiptMapper;
import com.tms.pod.service.PodService;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pod")
@RequiredArgsConstructor
public class PodController {
    private final PodReceiptMapper mapper;
    private final PodService service;

    @GetMapping("/page")
    public R<Page<PodReceipt>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status) {
        return R.ok(
                mapper.selectPage(
                        new Page<>(current, size),
                        new QueryWrapper<PodReceipt>()
                                .eq(status != null, "status", status)
                                .orderByDesc("id")));
    }

    @PostMapping("/{id}/return")
    public R<PodReceipt> returnPod(@PathVariable Long id, @RequestBody(required = false) ImageReq req) {
        return R.ok(service.change(id, "RETURNED", req == null ? null : req.imageUrl));
    }

    @PostMapping("/{id}/archive")
    public R<PodReceipt> archive(@PathVariable Long id) {
        return R.ok(service.change(id, "ARCHIVED", null));
    }

    @PostMapping("/{id}/lost")
    public R<PodReceipt> lost(@PathVariable Long id) {
        return R.ok(service.change(id, "LOST", null));
    }

    @GetMapping("/summary")
    public R<Map<String, Long>> summary() {
        return R.ok(service.summary());
    }

    @Data
    public static class ImageReq {
        private String imageUrl;
    }
}
