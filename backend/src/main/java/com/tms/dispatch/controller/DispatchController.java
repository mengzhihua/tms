package com.tms.dispatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tms.common.R;
import com.tms.dispatch.service.CarrierAdvisor;
import com.tms.dispatch.service.CarrierScoreService;
import com.tms.dispatch.service.MapBindService;
import com.tms.dispatch.service.MapBinder;
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
    private final CarrierScoreService scoreService;
    private final MapBindService mapBindService;

    @GetMapping("/pending-orders")
    public R<List<TransportOrder>> pending(@RequestParam(required = false) String fromSiteCode) {
        QueryWrapper<TransportOrder> q = new QueryWrapper<TransportOrder>().eq("status", "CREATED");
        if (fromSiteCode != null && !fromSiteCode.isEmpty()) {
            q.eq("from_site_code", fromSiteCode);
        }
        q.orderByDesc("priority");
        return R.ok(mapper.selectList(q));
    }

    /** preference: FAST 时效优先 / CHEAP 成本优先 / BALANCE 两天内选更低费率 / SCORE 按历史评分。空值按平衡。 */
    @GetMapping("/recommend")
    public R<CarrierAdvisor.Advice> recommend(@RequestParam(required = false) String preference) {
        if (preference != null && "SCORE".equalsIgnoreCase(preference.trim())) {
            return R.ok(scoreService.recommend());
        }
        return R.ok(CarrierAdvisor.advise(preference));
    }

    /** 按订单位置把待派订单绑到最近且装得下的空闲车。 */
    @GetMapping("/map-bind")
    public R<List<MapBinder.Assignment>> mapBind(@RequestParam(required = false) String fromSiteCode) {
        return R.ok(mapBindService.bind(fromSiteCode));
    }
}
