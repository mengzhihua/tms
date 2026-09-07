package com.tms.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.common.BizException;
import com.tms.common.CodeGenerator;
import com.tms.basic.entity.PackageMaterial;
import com.tms.basic.mapper.PackageMaterialMapper;
import com.tms.basic.service.RegionService;
import com.tms.order.entity.TransportOrder;
import com.tms.order.entity.TransportOrderLine;
import com.tms.order.mapper.TransportOrderLineMapper;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.tracking.entity.TrackingEvent;
import com.tms.tracking.mapper.TrackingEventMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final PackageMaterialMapper packageMapper;
    private final RegionService regionService;
    private final TrackingEventMapper eventMapper;

    @Transactional
    public TransportOrder create(TransportOrder o) {
        validate(o);
        o.setId(null);
        o.setCode(codeGenerator.next("TO"));
        o.setStatus("CREATED");
        if (o.getVolumeRatio() == null) {
            o.setVolumeRatio(new BigDecimal("6000"));
        }
        if (o.getPriority() == null) {
            o.setPriority(5);
        }
        enrich(o);
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
        enrich(input);
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

    @Transactional
    public TransportOrder reverse(Long id, String reason) {
        TransportOrder original = load(id);
        if (!"DELIVERED".equals(original.getStatus())) {
            throw new BizException("仅DELIVERED订单可生成退货单");
        }
        TransportOrder reverse = new TransportOrder();
        reverse.setCustomerCode(original.getCustomerCode());
        reverse.setOrderType("RETURN");
        reverse.setFromSiteCode(original.getFromSiteCode());
        reverse.setConsignorName(original.getConsigneeName());
        reverse.setConsignorPhone(original.getConsigneePhone());
        reverse.setConsignorAddress(original.getConsigneeAddress());
        reverse.setConsignorLng(original.getConsigneeLng());
        reverse.setConsignorLat(original.getConsigneeLat());
        reverse.setConsigneeName(original.getConsignorName());
        reverse.setConsigneePhone(original.getConsignorPhone());
        reverse.setConsigneeAddress(original.getConsignorAddress());
        reverse.setConsigneeLng(original.getConsignorLng());
        reverse.setConsigneeLat(original.getConsignorLat());
        reverse.setConsigneeProvince(original.getConsigneeProvince());
        reverse.setConsigneeCity(original.getConsigneeCity());
        reverse.setServiceLevelCode(original.getServiceLevelCode());
        reverse.setOriginOrderCode(original.getCode());
        reverse.setRemark(reason);
        reverse.setLines(original.getLines());
        TransportOrder created = create(reverse);
        TrackingEvent event = new TrackingEvent();
        event.setOrderId(original.getId());
        event.setWaybillId(original.getWaybillId());
        event.setWaybillCode(original.getWaybillCode());
        event.setEventType("RETURN_CREATED");
        event.setDescription(reason);
        event.setEventTime(LocalDateTime.now());
        event.setSource("SYSTEM");
        eventMapper.insert(event);
        return created;
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
            fillPackage(l);
            VolumeService.LineResult lr = VolumeService.calcLine(l);
            l.setLineVolumeM3(lr.getLineVolumeM3());
            l.setLineWeightKg(lr.getLineWeightKg());
            lineMapper.insert(l);
            i++;
        }
    }

    private void enrich(TransportOrder o) {
        if (o.getRegionCode() == null) {
            o.setRegionCode(regionService.match(o.getConsigneeProvince(), o.getConsigneeCity()));
        }
    }

    private void fillPackage(TransportOrderLine line) {
        if (line.getPackageCode() == null) {
            return;
        }
        PackageMaterial material =
                packageMapper.selectOne(
                        new LambdaQueryWrapper<PackageMaterial>()
                                .eq(PackageMaterial::getCode, line.getPackageCode()));
        if (material == null) {
            throw new BizException("包材不存在");
        }
        if (line.getLengthCm() == null) {
            line.setLengthCm(material.getLengthCm());
        }
        if (line.getWidthCm() == null) {
            line.setWidthCm(material.getWidthCm());
        }
        if (line.getHeightCm() == null) {
            line.setHeightCm(material.getHeightCm());
        }
        BigDecimal tare = material.getTareWeightKg() == null ? BigDecimal.ZERO : material.getTareWeightKg();
        BigDecimal weight = line.getWeightKg() == null ? BigDecimal.ZERO : line.getWeightKg();
        line.setWeightKg(weight.add(tare));
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
