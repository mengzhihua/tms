package com.tms.selection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.basic.entity.Carrier;
import com.tms.basic.entity.CarrierCoverage;
import com.tms.basic.entity.ServiceLevel;
import com.tms.basic.mapper.CarrierCoverageMapper;
import com.tms.basic.mapper.CarrierMapper;
import com.tms.basic.mapper.ServiceLevelMapper;
import com.tms.billing.service.BillingService;
import com.tms.common.BizException;
import com.tms.order.entity.TransportOrder;
import com.tms.order.mapper.TransportOrderMapper;
import com.tms.selection.entity.SelectionRule;
import com.tms.selection.mapper.SelectionRuleMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarrierSelectionService {
    private final TransportOrderMapper orderMapper;
    private final CarrierMapper carrierMapper;
    private final CarrierCoverageMapper coverageMapper;
    private final ServiceLevelMapper levelMapper;
    private final SelectionRuleMapper ruleMapper;
    private final BillingService billingService;

    public List<Candidate> recommend(Long orderId) {
        TransportOrder order = require(orderId);
        List<Carrier> carriers =
                carrierMapper.selectList(
                        new LambdaQueryWrapper<Carrier>().eq(Carrier::getStatus, "ENABLED"));
        List<Candidate> candidates = new ArrayList<>();
        for (Carrier carrier : carriers) {
            CarrierCoverage coverage = coverage(carrier.getCode(), order);
            if (coverage == null) {
                continue;
            }
            Candidate candidate = new Candidate();
            candidate.carrierCode = carrier.getCode();
            candidate.carrierName = carrier.getName();
            candidate.carrierType = carrier.getType();
            candidate.hours = hours(coverage, order);
            try {
                candidate.cost =
                        billingService
                                .calc(
                                        carrier.getCode(),
                                        "WEIGHT",
                                        order,
                                        BigDecimal.ZERO,
                                        order.getRegionCode(),
                                        order.getServiceLevelCode())
                                .getAmount();
            } catch (RuntimeException ex) {
                candidate.cost = null;
            }
            candidate.reason = candidate.cost == null ? "无可用区域价格" : "覆盖区域与时效";
            candidates.add(candidate);
        }
        SelectionRule rule = rule(order);
        sort(candidates, rule);
        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).recommended = i == 0;
        }
        return candidates;
    }

    @Transactional
    public TransportOrder assign(Long orderId, String carrierCode) {
        TransportOrder order = require(orderId);
        if (!"CREATED".equals(order.getStatus())) {
            throw new BizException("仅CREATED订单可分配承运商");
        }
        boolean exists =
                !carrierMapper
                        .selectList(
                                new LambdaQueryWrapper<Carrier>()
                                        .eq(Carrier::getCode, carrierCode)
                                        .eq(Carrier::getStatus, "ENABLED"))
                        .isEmpty();
        if (!exists) {
            throw new BizException("承运商不存在或未启用");
        }
        order.setCarrierCode(carrierCode);
        order.setRecommendRemark("智能筛单分配");
        orderMapper.updateById(order);
        return order;
    }

    @Transactional
    public List<TransportOrder> autoAssign(List<Long> orderIds) {
        List<TransportOrder> result = new ArrayList<>();
        for (Long id : orderIds) {
            List<Candidate> candidates = recommend(id);
            if (!candidates.isEmpty()) {
                result.add(assign(id, candidates.get(0).carrierCode));
            }
        }
        return result;
    }

    private CarrierCoverage coverage(String carrierCode, TransportOrder order) {
        List<CarrierCoverage> rows =
                coverageMapper.selectList(
                        new LambdaQueryWrapper<CarrierCoverage>()
                                .eq(CarrierCoverage::getCarrierCode, carrierCode)
                                .eq(CarrierCoverage::getStatus, "ENABLED"));
        for (CarrierCoverage row : rows) {
            if (order.getRegionCode() == null) {
                return row;
            }
            boolean region = order.getRegionCode() == null || row.getRegionCode() == null
                    || order.getRegionCode().equals(row.getRegionCode());
            boolean level = order.getServiceLevelCode() == null
                    || row.getServiceLevelCode() == null
                    || order.getServiceLevelCode().equals(row.getServiceLevelCode());
            if (region && level) {
                return row;
            }
        }
        return null;
    }

    private BigDecimal hours(CarrierCoverage coverage, TransportOrder order) {
        if (coverage.getPromisedHours() != null) {
            return coverage.getPromisedHours();
        }
        if (order.getServiceLevelCode() == null) {
            return null;
        }
        ServiceLevel level =
                levelMapper.selectOne(
                        new LambdaQueryWrapper<ServiceLevel>()
                                .eq(ServiceLevel::getCode, order.getServiceLevelCode()));
        return level == null ? null : level.getPromisedHours();
    }

    private SelectionRule rule(TransportOrder order) {
        List<SelectionRule> rules =
                ruleMapper.selectList(
                        new LambdaQueryWrapper<SelectionRule>()
                                .eq(SelectionRule::getStatus, "ENABLED")
                                .orderByAsc(SelectionRule::getPriority));
        for (SelectionRule rule : rules) {
            if (match(rule.getRegionCode(), order.getRegionCode())
                    && match(rule.getCustomerCode(), order.getCustomerCode())
                    && match(rule.getServiceLevelCode(), order.getServiceLevelCode())) {
                return rule;
            }
        }
        return null;
    }

    private boolean match(String expected, String actual) {
        return expected == null || expected.isEmpty() || expected.equals(actual);
    }

    private void sort(List<Candidate> candidates, SelectionRule rule) {
        String strategy = rule == null ? "COST" : rule.getStrategy();
        String designated = rule == null ? null : rule.getDesignatedCarrier();
        Comparator<Candidate> comparator =
                "TIME".equals(strategy)
                        ? Comparator.comparing(
                                Candidate::getHours,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        : Comparator.comparing(
                                Candidate::getCost,
                                Comparator.nullsLast(Comparator.naturalOrder()));
        if ("DESIGNATED".equals(strategy)) {
            comparator =
                    Comparator.comparing(
                                    (Candidate item) -> !item.getCarrierCode().equals(designated))
                            .thenComparing(
                                    Candidate::getCost,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
        }
        candidates.sort(comparator);
    }

    private TransportOrder require(Long id) {
        TransportOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    @Data
    public static class Candidate {
        private String carrierCode;
        private String carrierName;
        private String carrierType;
        private BigDecimal cost;
        private BigDecimal hours;
        private String reason;
        private boolean recommended;
    }
}
