package com.tms.selection.controller;

import com.tms.common.R;
import com.tms.order.entity.TransportOrder;
import com.tms.selection.service.CarrierSelectionService;
import com.tms.selection.service.CarrierSelectionService.Candidate;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/selection")
@RequiredArgsConstructor
public class SelectionController {
    private final CarrierSelectionService service;

    @GetMapping("/recommend/{orderId}")
    public R<List<Candidate>> recommend(@PathVariable Long orderId) {
        return R.ok(service.recommend(orderId));
    }

    @PostMapping("/assign/{orderId}")
    public R<TransportOrder> assign(
            @PathVariable Long orderId, @RequestParam String carrierCode) {
        return R.ok(service.assign(orderId, carrierCode));
    }

    @PostMapping("/auto-assign")
    public R<List<TransportOrder>> autoAssign(@RequestBody Ids req) {
        return R.ok(service.autoAssign(req.ids));
    }

    @Data
    public static class Ids {
        private List<Long> ids;
    }
}
