package com.tms.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.basic.entity.RateRule;
import com.tms.basic.mapper.RateRuleMapper;
import com.tms.billing.entity.FreightBill;
import com.tms.billing.mapper.FreightBillMapper;
import com.tms.common.BizException;
import com.tms.common.CodeGenerator;
import com.tms.order.entity.TransportOrder;
import com.tms.order.service.VolumeService;
import java.math.*;
import java.util.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BillingService {
    private final RateRuleMapper ruleMapper;
    private final FreightBillMapper billMapper;
    private final CodeGenerator codeGenerator;

    public CalcResult calc(
            String carrierCode, String chargeType, TransportOrder order, BigDecimal distanceKm) {
        String type = chargeType;
        List<RateRule> all =
                ruleMapper.selectList(
                        new LambdaQueryWrapper<RateRule>()
                                .eq(RateRule::getStatus, "ENABLED")
                                .and(
                                        w ->
                                                w.eq(RateRule::getCarrierCode, carrierCode)
                                                        .or()
                                                        .isNull(RateRule::getCarrierCode))
                                .orderByAsc(RateRule::getPriority));
        RateRule chosen = null;
        for (RateRule r : all) {
            if (type == null || type.equals(r.getChargeType())) {
                if (chosen == null || Objects.equals(r.getCarrierCode(), carrierCode)) {
                    chosen = r;
                }
                if (Objects.equals(r.getCarrierCode(), carrierCode)) break;
            }
        }
        if (chosen == null) {
            throw new BizException("无可用计费规则");
        }
        if (type == null) {
            type = chosen.getChargeType();
        }
        BigDecimal quantity = quantity(type, order, distanceKm, chosen.getVolumeRatio());
        BigDecimal extra =
                quantity.compareTo(nz(chosen.getFirstUnit())) <= 0
                        ? BigDecimal.ZERO
                        : quantity
                                .subtract(chosen.getFirstUnit())
                                .divide(nz(chosen.getAddUnit()), 0, RoundingMode.CEILING);
        BigDecimal amount = nz(chosen.getFirstPrice()).add(extra.multiply(nz(chosen.getAddPrice())));
        if (chosen.getMinCharge() != null && amount.compareTo(chosen.getMinCharge()) < 0) {
            amount = chosen.getMinCharge();
        }
        CalcResult r = new CalcResult();
        r.rule = chosen;
        r.quantity = quantity;
        r.amount = amount;
        r.calcDetail = detail(chosen, quantity, extra);
        return r;
    }

    public BigDecimal createBill(com.tms.dispatch.entity.Waybill w, TransportOrder o) {
        return createBill(w, o, BigDecimal.ZERO);
    }

    public BigDecimal createBill(
            com.tms.dispatch.entity.Waybill w, TransportOrder o, BigDecimal distance) {
        CalcResult r = calc(w.getCarrierCode(), null, o, distance);
        FreightBill b = new FreightBill();
        b.setCode(codeGenerator.next("FB"));
        b.setWaybillId(w.getId());
        b.setWaybillCode(w.getCode());
        b.setOrderId(o.getId());
        b.setOrderCode(o.getCode());
        b.setCarrierCode(w.getCarrierCode());
        b.setRuleCode(r.rule.getCode());
        b.setChargeType(r.rule.getChargeType());
        b.setQuantity(r.quantity);
        b.setAmount(r.amount);
        b.setCalcDetail(r.calcDetail);
        b.setStatus("UNBILLED");
        billMapper.insert(b);
        return r.amount;
    }

    /** 换承运商产生的运费差额。正数是加价，负数是节约。零差额不落单。 */
    public FreightBill recordFreightDelta(
            com.tms.dispatch.entity.Waybill w,
            String fromCarrier,
            BigDecimal fromAmount,
            BigDecimal toAmount) {
        BigDecimal delta = freightDelta(fromAmount, toAmount);
        if (delta == null) {
            return null;
        }
        FreightBill b = new FreightBill();
        b.setCode(codeGenerator.next("FD"));
        b.setWaybillId(w.getId());
        b.setWaybillCode(w.getCode());
        b.setCarrierCode(w.getCarrierCode());
        b.setChargeType("FREIGHT_DELTA");
        b.setQuantity(BigDecimal.ONE);
        b.setAmount(delta);
        b.setStatus("UNBILLED");
        b.setCalcDetail(fromCarrier + "->" + w.getCarrierCode()
                + " " + fromAmount.toPlainString() + "->" + toAmount.toPlainString());
        billMapper.insert(b);
        return b;
    }

    public static BigDecimal freightDelta(BigDecimal fromAmount, BigDecimal toAmount) {
        if (fromAmount == null || toAmount == null) {
            return null;
        }
        BigDecimal delta = toAmount.subtract(fromAmount);
        return delta.signum() == 0 ? null : delta;
    }

    public List<FreightBill> bills(Long waybillId) {
        return billMapper.selectList(
                new LambdaQueryWrapper<FreightBill>().eq(FreightBill::getWaybillId, waybillId));
    }

    public void updateBill(FreightBill bill) {
        billMapper.updateById(bill);
    }

    public static BigDecimal quantity(
            String type, TransportOrder o, BigDecimal distance, BigDecimal ratio) {
        if ("DISTANCE".equals(type)) {
            return nz(distance);
        }
        if ("VOLUME".equals(type)) {
            return nz(o.getTotalVolumeM3());
        }
        if ("PIECE".equals(type)) {
            return nz(o.getTotalQty());
        }
        if (o.getLines() == null || o.getLines().isEmpty()) {
            return nz(o.getChargeableWeightKg());
        }
        BigDecimal ratio0 = ratio == null ? new BigDecimal("6000") : ratio;
        return VolumeService.calc(o.getLines(), ratio0).getChargeableWeightKg();
    }

    private static BigDecimal nz(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }

    private String detail(RateRule r, BigDecimal q, BigDecimal extra) {
        return "首单位"
                + r.getFirstUnit()
                + " "
                + r.getFirstPrice()
                + "元 + 续单位 "
                + extra
                + "×"
                + r.getAddPrice();
    }

    @Data
    public static class CalcResult {
        private RateRule rule;
        private BigDecimal quantity, amount;
        private String calcDetail;
    }
}
