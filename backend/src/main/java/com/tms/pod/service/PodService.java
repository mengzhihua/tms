package com.tms.pod.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.common.BizException;
import com.tms.common.CodeGenerator;
import com.tms.dispatch.entity.Waybill;
import com.tms.order.entity.TransportOrder;
import com.tms.pod.entity.PodReceipt;
import com.tms.pod.mapper.PodReceiptMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PodService {
    private final PodReceiptMapper mapper;
    private final CodeGenerator codeGenerator;

    public PodReceipt create(Waybill waybill, TransportOrder order, String image) {
        PodReceipt item = new PodReceipt();
        item.setCode(codeGenerator.next("POD"));
        item.setWaybillId(waybill.getId());
        item.setWaybillCode(waybill.getCode());
        item.setOrderId(order.getId());
        item.setOrderCode(order.getCode());
        item.setCarrierCode(waybill.getCarrierCode());
        item.setSigner(order.getSigner());
        item.setSignTime(order.getSignTime());
        item.setImageUrl(image);
        item.setReceiptType(image == null ? "PAPER" : "ELECTRONIC");
        item.setStatus(image == null ? "PENDING" : "RETURNED");
        if (image != null) {
            item.setReturnedTime(LocalDateTime.now());
        }
        mapper.insert(item);
        return item;
    }

    public PodReceipt change(Long id, String status, String imageUrl) {
        PodReceipt item = mapper.selectById(id);
        if (item == null) {
            throw new BizException("回单不存在");
        }
        item.setStatus(status);
        if (imageUrl != null) {
            item.setImageUrl(imageUrl);
        }
        if ("RETURNED".equals(status)) {
            item.setReturnedTime(LocalDateTime.now());
        }
        if ("ARCHIVED".equals(status)) {
            item.setArchivedTime(LocalDateTime.now());
        }
        mapper.updateById(item);
        return item;
    }

    public Map<String, Long> summary() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (PodReceipt item : mapper.selectList(null)) {
            result.put(item.getStatus(), result.getOrDefault(item.getStatus(), 0L) + 1);
        }
        return result;
    }
}
