package com.tms.openapi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.basic.entity.Customer;
import com.tms.common.CodeGenerator;
import com.tms.common.R;
import com.tms.order.entity.TransportOrder;
import com.tms.order.service.TransportOrderService;
import com.tms.openapi.entity.PushLog;
import com.tms.openapi.mapper.PushLogMapper;
import com.tms.tracking.entity.TrackingEvent;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
public class OpenApiController {
    private final TransportOrderService orderService;
    private final com.tms.order.mapper.TransportOrderMapper orderMapper;
    private final TrackingEventMapper eventMapper;
    private final CodeGenerator codeGenerator;

    @PostMapping("/orders")
    public R<List<Map<String, Object>>> orders(
            @RequestAttribute("openCustomer") Customer customer,
            @RequestBody List<TransportOrder> inputs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (TransportOrder input : inputs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sourceNo", input.getSourceNo());
            if (orderMapper.selectOne(
                            new LambdaQueryWrapper<TransportOrder>()
                                    .eq(TransportOrder::getCustomerCode, customer.getCode())
                                    .eq(TransportOrder::getSourceNo, input.getSourceNo()))
                    != null) {
                item.put("success", false);
                item.put("message", "重复推单");
            } else {
                input.setCustomerCode(customer.getCode());
                TransportOrder created = orderService.create(input);
                item.put("code", created.getCode());
                item.put("success", true);
                item.put("message", "success");
            }
            result.add(item);
        }
        return R.ok(result);
    }

    @GetMapping("/orders/{code}/track")
    public R<Map<String, Object>> track(@PathVariable String code) {
        return R.ok(trackData(code));
    }

    @GetMapping("/orders/track")
    public R<Map<String, Object>> trackBySource(@RequestParam String sourceNo) {
        TransportOrder order =
                orderMapper.selectOne(new LambdaQueryWrapper<TransportOrder>().eq(TransportOrder::getSourceNo, sourceNo));
        if (order == null) {
            return R.fail(404, "订单不存在");
        }
        return R.ok(trackData(order.getCode()));
    }

    private Map<String, Object> trackData(String code) {
        TransportOrder order =
                orderMapper.selectOne(new LambdaQueryWrapper<TransportOrder>().eq(TransportOrder::getCode, code));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("order", order);
        data.put(
                "events",
                order == null
                        ? new ArrayList<>()
                        : eventMapper.selectList(
                                new LambdaQueryWrapper<TrackingEvent>()
                                        .eq(TrackingEvent::getOrderId, order.getId())
                                        .orderByAsc(TrackingEvent::getEventTime)));
        return data;
    }
}
