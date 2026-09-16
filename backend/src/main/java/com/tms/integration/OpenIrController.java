package com.tms.integration;

import com.tms.common.BizException;
import com.tms.common.R;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.service.DispatchService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** IR 控制塔按运单号调度、拉轨迹、换承运商。 */
@RestController
@RequestMapping("/api/open/ir")
@RequiredArgsConstructor
public class OpenIrController {
    private final DispatchService dispatchService;

    @Data
    public static class CodeReq {
        private String waybillCode;
        private String targetKey;
        private String carrierCode;
        private String type;
        private Map<String, Object> params;
    }

    @PostMapping("/dispatch")
    public R<Waybill> dispatch(@RequestBody CodeReq req) {
        return R.ok(dispatchService.dispatchByCode(code(req)));
    }

    @PostMapping("/sync-track")
    public R<Waybill> syncTrack(@RequestBody CodeReq req) {
        return R.ok(dispatchService.syncTrackByCode(code(req)));
    }

    @PostMapping("/switch-carrier")
    public R<Waybill> switchCarrier(@RequestBody CodeReq req) {
        return R.ok(dispatchService.switchCarrierByCode(code(req), carrier(req)));
    }

    @PostMapping("/actions")
    public R<Waybill> actions(@RequestBody CodeReq req) {
        String type = req.getType() == null ? "" : req.getType();
        if ("TMS_DISPATCH".equals(type)) {
            return R.ok(dispatchService.dispatchByCode(code(req)));
        }
        if ("TMS_SYNC_TRACK".equals(type)) {
            return R.ok(dispatchService.syncTrackByCode(code(req)));
        }
        if ("TMS_SWITCH_CARRIER".equals(type)) {
            return R.ok(dispatchService.switchCarrierByCode(code(req), carrier(req)));
        }
        throw new BizException("不支持的 IR 指令: " + type);
    }

    private String code(CodeReq req) {
        String value = first(req.getWaybillCode(), req.getTargetKey());
        if (value == null) {
            throw new BizException("waybillCode 必填");
        }
        return value;
    }

    private String carrier(CodeReq req) {
        String value = req.getCarrierCode();
        if ((value == null || value.trim().isEmpty()) && req.getParams() != null) {
            Object param = req.getParams().get("carrierCode");
            value = param == null ? null : String.valueOf(param);
        }
        return value;
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && !"null".equals(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
