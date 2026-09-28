package com.tms.exc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.common.BizException;
import com.tms.common.CodeGenerator;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.exc.entity.TransportException;
import com.tms.exc.mapper.TransportExceptionMapper;
import com.tms.openapi.RoutePushService;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.tracking.entity.TrackingEvent;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExceptionService {
    private final TransportExceptionMapper mapper;
    private final WaybillMapper waybillMapper;
    private final TrackingEventMapper eventMapper;
    private final CodeGenerator codeGenerator;
    private final TransportOrderMapper orderMapper;
    private final RoutePushService routePushService;

    @Transactional
    public TransportException create(TransportException input) {
        input.setId(null);
        input.setCode(codeGenerator.next("EX"));
        if (input.getStatus() == null) {
            input.setStatus("OPEN");
        }
        if (input.getSource() == null) {
            input.setSource("MANUAL");
        }
        if (input.getClaimStatus() == null) {
            input.setClaimStatus("NONE");
        }
        input.setClaimFlag(Boolean.TRUE.equals(input.getClaimFlag()));
        mapper.insert(input);
        if (input.getOrderId() != null) {
            TransportOrder order = orderMapper.selectById(input.getOrderId());
            if (order != null) {
                routePushService.push(
                        order.getCustomerCode(),
                        order.getCode(),
                        order.getSourceNo(),
                        "EXCEPTION",
                        "EXCEPTION",
                        input.getDescription(),
                        input.getWaybillId() == null
                                ? null
                                : waybillMapper.selectById(input.getWaybillId()));
            }
        }
        if (input.getWaybillId() != null) {
            Waybill waybill = waybillMapper.selectById(input.getWaybillId());
            if (waybill != null) {
                waybill.setExceptionFlag(true);
                waybillMapper.updateById(waybill);
            }
            TrackingEvent event = new TrackingEvent();
            event.setWaybillId(input.getWaybillId());
            event.setWaybillCode(input.getWaybillCode());
            event.setOrderId(input.getOrderId());
            event.setEventType("EXCEPTION");
            event.setDescription(input.getDescription());
            event.setEventTime(LocalDateTime.now());
            event.setSource(input.getSource());
            eventMapper.insert(event);
        }
        return input;
    }

    public TransportException handle(Long id, HandleReq req) {
        TransportException item = require(id);
        item.setStatus(req.status);
        item.setHandler(req.handler);
        item.setHandleRemark(req.remark);
        item.setHandleTime(LocalDateTime.now());
        mapper.updateById(item);
        return item;
    }

    public TransportException claim(Long id, ClaimReq req) {
        TransportException item = require(id);
        item.setClaimFlag(true);
        item.setClaimAmount(req.amount);
        item.setClaimRemark(req.remark);
        item.setClaimStatus("APPLIED");
        mapper.updateById(item);
        return item;
    }

    public TransportException audit(Long id, ClaimReq req) {
        TransportException item = require(id);
        item.setClaimStatus(req.approve ? "APPROVED" : "REJECTED");
        item.setClaimRemark(req.remark);
        mapper.updateById(item);
        return item;
    }

    public TransportException pay(Long id) {
        TransportException item = require(id);
        if (!"APPROVED".equals(item.getClaimStatus())) {
            throw new BizException("理赔未审核通过");
        }
        item.setClaimStatus("PAID");
        mapper.updateById(item);
        return item;
    }

    public int scan(LocalDateTime now) {
        int created = 0;
        List<Waybill> waybills = waybillMapper.selectList(null);
        for (Waybill waybill : waybills) {
            if ("DISPATCHED".equals(waybill.getStatus())
                    && waybill.getPlannedDepartTime() != null
                    && now.isAfter(waybill.getPlannedDepartTime().plusMinutes(30))) {
                created += timeout(waybill, "TIMEOUT_DEPART", "MEDIUM", "超过计划发车时间");
            }
            if ("IN_TRANSIT".equals(waybill.getStatus())
                    && waybill.getPromisedArriveTime() != null
                    && now.isAfter(waybill.getPromisedArriveTime())) {
                created += timeout(waybill, "TIMEOUT_ARRIVE", "HIGH", "超过承诺到达时间");
            }
            if ("ARRIVED".equals(waybill.getStatus())
                    && waybill.getActualArriveTime() != null
                    && now.isAfter(waybill.getActualArriveTime().plusHours(24))) {
                created += timeout(waybill, "TIMEOUT_SIGN", "HIGH", "到达后超过24小时未签收");
            }
        }
        return created;
    }

    public Map<String, Long> summary() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (TransportException item :
                mapper.selectList(
                        new LambdaQueryWrapper<TransportException>()
                                .ne(TransportException::getStatus, "CLOSED"))) {
            String key = item.getType() + ":" + item.getLevel();
            result.put(key, result.getOrDefault(key, 0L) + 1);
        }
        return result;
    }

    private int timeout(Waybill waybill, String type, String level, String description) {
        Long count =
                mapper.selectCount(
                        new LambdaQueryWrapper<TransportException>()
                                .eq(TransportException::getWaybillId, waybill.getId())
                                .eq(TransportException::getType, type)
                                .ne(TransportException::getStatus, "CLOSED"));
        if (count > 0) {
            return 0;
        }
        TransportException item = new TransportException();
        item.setWaybillId(waybill.getId());
        item.setWaybillCode(waybill.getCode());
        item.setCarrierCode(waybill.getCarrierCode());
        item.setType(type);
        item.setLevel(level);
        item.setDescription(description);
        item.setSource("SYSTEM");
        create(item);
        return 1;
    }

    private TransportException require(Long id) {
        TransportException item = mapper.selectById(id);
        if (item == null) {
            throw new BizException("异常不存在");
        }
        return item;
    }

    @Data
    public static class HandleReq {
        private String status;
        private String handler;
        private String remark;
    }

    @Data
    public static class ClaimReq {
        private BigDecimal amount;
        private String remark;
        private boolean approve;
    }
}
