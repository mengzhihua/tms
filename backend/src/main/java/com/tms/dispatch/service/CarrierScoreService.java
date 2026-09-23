package com.tms.dispatch.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.billing.entity.FreightBill;
import com.tms.billing.mapper.FreightBillMapper;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 用已送达或已关闭运单给目录里的承运商记分，并在评分优先时给出建议。 */
@Service
@RequiredArgsConstructor
public class CarrierScoreService {
    private final WaybillMapper waybillMapper;
    private final FreightBillMapper billMapper;

    public List<CarrierScore.Result> scoreAll() {
        List<Waybill> done =
                waybillMapper.selectList(
                        new LambdaQueryWrapper<Waybill>()
                                .in(Waybill::getStatus, Arrays.asList("DELIVERED", "CLOSED")));
        Map<String, List<CarrierScore.Trip>> trips = new LinkedHashMap<String, List<CarrierScore.Trip>>();
        for (Waybill waybill : done) {
            if (waybill.getCarrierCode() == null || waybill.getCarrierCode().trim().isEmpty()) {
                continue;
            }
            String code = waybill.getCarrierCode().trim();
            List<CarrierScore.Trip> bucket = trips.get(code);
            if (bucket == null) {
                bucket = new ArrayList<CarrierScore.Trip>();
                trips.put(code, bucket);
            }
            bucket.add(
                    CarrierScore.trip(
                            waybill.getExceptionFlag(),
                            waybill.getPlannedArriveTime(),
                            waybill.getActualArriveTime(),
                            quoted(waybill.getId()),
                            waybill.getFreightAmount()));
        }
        List<CarrierScore.Result> results = new ArrayList<CarrierScore.Result>();
        for (CarrierAdvisor.Option option : CarrierAdvisor.catalog()) {
            results.add(CarrierScore.score(option.code, trips.get(option.code)));
        }
        return results;
    }

    public CarrierAdvisor.Advice recommend() {
        CarrierScore.Result best = CarrierScore.best(scoreAll());
        List<CarrierAdvisor.Option> options = CarrierAdvisor.catalog();
        if (best == null) {
            CarrierAdvisor.Advice balance = CarrierAdvisor.advise(CarrierAdvisor.BALANCE);
            CarrierAdvisor.Option chosen = option(options, balance.getCarrierCode());
            return new CarrierAdvisor.Advice(
                    "SCORE", chosen, "还没有完成运单，改用平衡。" + balance.getReason(), options);
        }
        CarrierAdvisor.Option chosen = option(options, best.getCarrierCode());
        String reason =
                "评分优先：选"
                        + chosen.getName()
                        + " "
                        + best.getScore().toPlainString()
                        + " 分，准时 "
                        + CarrierScore.percent(best.getOnTimeRate())
                        + "，异常 "
                        + CarrierScore.percent(best.getExceptionRate())
                        + "，费用偏差 "
                        + CarrierScore.percent(best.getCostVariance());
        return new CarrierAdvisor.Advice("SCORE", chosen, reason, options);
    }

    private BigDecimal quoted(Long waybillId) {
        if (waybillId == null) {
            return BigDecimal.ZERO;
        }
        List<FreightBill> bills =
                billMapper.selectList(
                        new LambdaQueryWrapper<FreightBill>().eq(FreightBill::getWaybillId, waybillId));
        BigDecimal sum = BigDecimal.ZERO;
        for (FreightBill bill : bills) {
            if ("FREIGHT_DELTA".equals(bill.getChargeType()) || bill.getAmount() == null) {
                continue;
            }
            sum = sum.add(bill.getAmount());
        }
        return sum;
    }

    private static CarrierAdvisor.Option option(List<CarrierAdvisor.Option> options, String code) {
        for (CarrierAdvisor.Option option : options) {
            if (option.getCode().equals(code)) {
                return option;
            }
        }
        return options.get(0);
    }
}
