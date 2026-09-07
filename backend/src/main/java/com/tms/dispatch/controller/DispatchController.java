package com.tms.dispatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tms.common.R;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {
    private final TransportOrderMapper mapper;

    @GetMapping("/pending-orders")
    public R<List<TransportOrder>> pending(
            @RequestParam(required = false) String fromSiteCode,
            @RequestParam(required = false) String carrierCode) {
        QueryWrapper<TransportOrder> q = new QueryWrapper<TransportOrder>().eq("status", "CREATED");
        if (fromSiteCode != null && !fromSiteCode.isEmpty()) {
            q.eq("from_site_code", fromSiteCode);
        }
        if (carrierCode != null && !carrierCode.isEmpty()) {
            q.and(w -> w.eq("carrier_code", carrierCode).or().isNull("carrier_code"));
        }
        q.orderByDesc("priority");
        return R.ok(mapper.selectList(q));
    }
}
