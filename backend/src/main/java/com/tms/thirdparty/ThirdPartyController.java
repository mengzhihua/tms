package com.tms.thirdparty;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.common.BizException;
import com.tms.common.R;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.dispatch.service.DispatchService;
import com.tms.tracking.entity.TrackingEvent;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thirdparty")
@RequiredArgsConstructor
public class ThirdPartyController {
    private final WaybillMapper waybillMapper;
    private final TrackingEventMapper eventMapper;
    private final DispatchService dispatchService;

    @PostMapping("/callback/{provider}")
    public R<Void> callback(@PathVariable String provider, @RequestBody CallbackReq r) {
        Waybill w =
                waybillMapper.selectOne(
                        new LambdaQueryWrapper<Waybill>().eq(Waybill::getThirdPartyNo, r.thirdPartyNo));
        if (w == null) {
            throw new BizException("三方运单不存在");
        }
        w.setThirdPartyStatus(r.status);
        TrackingEvent e = new TrackingEvent();
        e.setWaybillId(w.getId());
        e.setWaybillCode(w.getCode());
        e.setEventType("THIRD_PARTY");
        e.setDescription(r.description);
        e.setLng(r.lng);
        e.setLat(r.lat);
        e.setEventTime(r.eventTime == null ? LocalDateTime.now() : r.eventTime);
        e.setSource("THIRD_PARTY");
        eventMapper.insert(e);
        if ("SIGNED".equals(r.status)) {
            dispatchService.completeThirdParty(w.getId());
            w = dispatchService.require(w.getId());
        }
        w.setThirdPartyStatus(r.status);
        waybillMapper.updateById(w);
        return R.ok();
    }

    @Data
    public static class CallbackReq {
        private String thirdPartyNo, status, description;
        private BigDecimal lng, lat;
        private LocalDateTime eventTime;
    }
}
