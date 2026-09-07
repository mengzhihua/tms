package com.tms.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.common.BizException;
import com.tms.common.CodeGenerator;
import com.tms.order.entity.TransportOrder;
import com.tms.order.entity.TransportOrderLine;
import com.tms.order.mapper.TransportOrderLineMapper;
import com.tms.order.mapper.TransportOrderMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransportOrderService {
    private final TransportOrderMapper orderMapper;
    private final TransportOrderLineMapper lineMapper;
    private final CodeGenerator codeGenerator;
    private final VolumeService volumeService;

    @Transactional
    public TransportOrder create(TransportOrder o) {
        validate(o);
        o.setId(null);
        o.setCode(codeGenerator.next("TO"));
        o.setStatus("CREATED");
        if (o.getVolumeRatio() == null) o.setVolumeRatio(new BigDecimal("6000"));
        if (o.getPriority() == null) o.setPriority(5);
        orderMapper.insert(o);
        saveLines(o);
        return load(o.getId());
    }

    @Transactional
    public TransportOrder update(Long id, TransportOrder input) {
        TransportOrder db = require(id);
        if (!"CREATED".equals(db.getStatus())) {
            throw new BizException("仅CREATED状态订单可修改");
        }
        validate(input);
        input.setId(id);
        input.setCode(db.getCode());
        input.setStatus(db.getStatus());
        input.setWaybillId(db.getWaybillId());
        input.setWaybillCode(db.getWaybillCode());
        orderMapper.updateById(input);
        lineMapper.delete(
                new LambdaQueryWrapper<TransportOrderLine>().eq(TransportOrderLine::getOrderId, id));
        saveLines(input);
        return load(id);
    }

    public TransportOrder load(Long id) {
        TransportOrder o = require(id);
        o.setLines(
                lineMapper.selectList(
                        new LambdaQueryWrapper<TransportOrderLine>()
                                .eq(TransportOrderLine::getOrderId, id)
                                .orderByAsc(TransportOrderLine::getId)));
        return o;
    }

    @Transactional
    public void cancel(Long id) {
        TransportOrder o = require(id);
        if (!"CREATED".equals(o.getStatus())) {
            throw new BizException("仅CREATED状态订单可取消");
        }
        o.setStatus("CANCELLED");
        orderMapper.updateById(o);
    }

    private void saveLines(TransportOrder o) {
        VolumeService.VolumeResult r = volumeService.calc(o.getLines(), o.getVolumeRatio());
        o.setTotalQty(r.getTotalQty());
        o.setTotalWeightKg(r.getTotalWeightKg());
        o.setTotalVolumeM3(r.getTotalVolumeM3());
        o.setVolumetricWeightKg(r.getVolumetricWeightKg());
        o.setChargeableWeightKg(r.getChargeableWeightKg());
        orderMapper.updateById(o);
        int i = 0;
        for (TransportOrderLine l : o.getLines()) {
            l.setId(null);
            l.setOrderId(o.getId());
            VolumeService.LineResult lr = VolumeService.calcLine(l);
            l.setLineVolumeM3(lr.getLineVolumeM3());
            l.setLineWeightKg(lr.getLineWeightKg());
            lineMapper.insert(l);
            i++;
        }
    }

    private void validate(TransportOrder o) {
        if (o.getLines() == null || o.getLines().isEmpty()) {
            throw new BizException("订单至少需要一行明细");
        }
        for (TransportOrderLine l : o.getLines()) {
            if (l.getQty() == null || l.getQty().signum() <= 0) {
                throw new BizException("明细数量必须大于0");
            }
        }
    }

    public TransportOrder require(Long id) {
        TransportOrder o = orderMapper.selectById(id);
        if (o == null) {
            throw new BizException("订单不存在");
        }
        return o;
    }
}
