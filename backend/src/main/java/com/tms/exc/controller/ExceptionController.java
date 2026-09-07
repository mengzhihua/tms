package com.tms.exc.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tms.common.R;
import com.tms.exc.entity.TransportException;
import com.tms.exc.mapper.TransportExceptionMapper;
import com.tms.exc.service.ExceptionService;
import com.tms.exc.service.ExceptionService.ClaimReq;
import com.tms.exc.service.ExceptionService.HandleReq;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exception")
@RequiredArgsConstructor
public class ExceptionController {
    private final TransportExceptionMapper mapper;
    private final ExceptionService service;

    @GetMapping("/page")
    public R<Page<TransportException>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String carrierCode,
            @RequestParam(required = false) String claimStatus) {
        QueryWrapper<TransportException> query = new QueryWrapper<>();
        query.eq(status != null, "status", status);
        query.eq(type != null, "type", type);
        query.eq(carrierCode != null, "carrier_code", carrierCode);
        query.eq(claimStatus != null, "claim_status", claimStatus);
        query.orderByDesc("id");
        return R.ok(mapper.selectPage(new Page<>(current, size), query));
    }

    @GetMapping("/{id}")
    public R<TransportException> get(@PathVariable Long id) {
        return R.ok(mapper.selectById(id));
    }

    @PostMapping
    public R<TransportException> create(@RequestBody TransportException item) {
        return R.ok(service.create(item));
    }

    @PostMapping("/{id}/handle")
    public R<TransportException> handle(@PathVariable Long id, @RequestBody HandleReq req) {
        return R.ok(service.handle(id, req));
    }

    @PostMapping("/{id}/claim")
    public R<TransportException> claim(@PathVariable Long id, @RequestBody ClaimReq req) {
        return R.ok(service.claim(id, req));
    }

    @PostMapping("/{id}/claim-audit")
    public R<TransportException> audit(@PathVariable Long id, @RequestBody ClaimReq req) {
        return R.ok(service.audit(id, req));
    }

    @PostMapping("/{id}/claim-pay")
    public R<TransportException> pay(@PathVariable Long id) {
        return R.ok(service.pay(id));
    }

    @PostMapping("/scan")
    public R<Integer> scan() {
        return R.ok(service.scan(LocalDateTime.now()));
    }

    @GetMapping("/summary")
    public R<Map<String, Long>> summary() {
        return R.ok(service.summary());
    }
}
