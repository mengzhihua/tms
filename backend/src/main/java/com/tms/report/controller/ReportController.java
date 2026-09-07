package com.tms.report.controller;

import com.tms.common.R;
import com.tms.report.service.ReportService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService service;

    @GetMapping("/sla")
    public R<List<Map<String, Object>>> sla(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String carrierCode) {
        return R.ok(service.sla(from, to, carrierCode));
    }

    @GetMapping("/sla-flow")
    public R<List<Map<String, Object>>> slaFlow(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String carrierCode) {
        return R.ok(service.slaFlow(from, to, carrierCode));
    }

    @GetMapping("/transit-sign")
    public R<List<Map<String, Object>>> transitSign(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String carrierCode) {
        return R.ok(service.transitSign(from, to, carrierCode));
    }

    @GetMapping("/quality")
    public R<List<Map<String, Object>>> quality(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String carrierCode) {
        return R.ok(service.quality(from, to, carrierCode));
    }

    @GetMapping("/order-structure")
    public R<Map<String, Object>> orderStructure(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String carrierCode) {
        return R.ok(service.orderStructure(from, to, carrierCode));
    }

    @GetMapping("/alert-summary")
    public R<Map<String, Object>> alertSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String carrierCode) {
        return R.ok(service.alertSummary(from, to, carrierCode));
    }
}
