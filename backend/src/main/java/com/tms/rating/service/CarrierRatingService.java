package com.tms.rating.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.basic.entity.Carrier;
import com.tms.basic.mapper.CarrierMapper;
import com.tms.common.BizException;
import com.tms.dispatch.entity.Waybill;
import com.tms.dispatch.mapper.WaybillMapper;
import com.tms.exc.entity.TransportException;
import com.tms.exc.mapper.TransportExceptionMapper;
import com.tms.pod.entity.PodReceipt;
import com.tms.pod.mapper.PodReceiptMapper;
import com.tms.rating.entity.CarrierRating;
import com.tms.rating.mapper.CarrierRatingMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarrierRatingService {
    private final WaybillMapper waybillMapper;
    private final CarrierMapper carrierMapper;
    private final TransportExceptionMapper exceptionMapper;
    private final PodReceiptMapper podMapper;
    private final CarrierRatingMapper ratingMapper;

    public List<CarrierRating> compute(String period) {
        String target = period == null ? YearMonth.now().toString() : period;
        LocalDate first = LocalDate.parse(target + "-01");
        LocalDateTime from = first.atStartOfDay();
        LocalDateTime to = first.plusMonths(1).atStartOfDay();
        List<CarrierRating> result = new ArrayList<>();
        for (Carrier carrier : carrierMapper.selectList(null)) {
            List<Waybill> rows =
                    waybillMapper.selectList(
                            new LambdaQueryWrapper<Waybill>()
                                    .eq(Waybill::getCarrierCode, carrier.getCode())
                                    .ge(Waybill::getActualDepartTime, from)
                                    .lt(Waybill::getActualDepartTime, to));
            CarrierRating item = calculate(carrier, rows, target);
            CarrierRating old =
                    ratingMapper.selectOne(
                            new LambdaQueryWrapper<CarrierRating>()
                                    .eq(CarrierRating::getCarrierCode, carrier.getCode())
                                    .eq(CarrierRating::getPeriod, target));
            if (old == null) {
                ratingMapper.insert(item);
            } else {
                item.setId(old.getId());
                ratingMapper.updateById(item);
            }
            result.add(item);
        }
        return result;
    }

    private CarrierRating calculate(Carrier carrier, List<Waybill> rows, String period) {
        int count = rows.size();
        int closed = 0;
        int onTime = 0;
        int onTimeDenom = 0;
        double transit = 0;
        int transitCount = 0;
        int exceptions = 0;
        for (Waybill row : rows) {
            if ("CLOSED".equals(row.getStatus())) {
                closed++;
            }
            if (row.getOnTime() != null) {
                onTimeDenom++;
                if (row.getOnTime()) {
                    onTime++;
                }
            }
            if (row.getActualDepartTime() != null && row.getActualArriveTime() != null) {
                transit += java.time.Duration.between(row.getActualDepartTime(), row.getActualArriveTime()).toMinutes() / 60.0;
                transitCount++;
            }
            exceptions +=
                    exceptionMapper.selectCount(
                            new LambdaQueryWrapper<TransportException>()
                                    .eq(TransportException::getWaybillId, row.getId()));
        }
        List<Long> waybillIds = new ArrayList<>();
        for (Waybill row : rows) {
            waybillIds.add(row.getId());
        }
        if (waybillIds.isEmpty()) {
            waybillIds.add(-1L);
        }
        int receipts =
                podMapper
                        .selectCount(
                                new LambdaQueryWrapper<PodReceipt>()
                                        .in(PodReceipt::getWaybillId, waybillIds))
                        .intValue();
        int returned =
                podMapper
                        .selectCount(
                                new LambdaQueryWrapper<PodReceipt>()
                                        .in(PodReceipt::getWaybillId, waybillIds)
                                        .in(PodReceipt::getStatus, "RETURNED", "ARCHIVED"))
                        .intValue();
        BigDecimal onTimeRate = ratio(onTime, onTimeDenom);
        BigDecimal exceptionRate = ratio(exceptions, count);
        BigDecimal podRate = ratio(returned, receipts);
        BigDecimal score =
                onTimeRate.multiply(new BigDecimal("50"))
                        .add(BigDecimal.ONE.subtract(exceptionRate.min(BigDecimal.ONE)).multiply(new BigDecimal("30")))
                        .add(podRate.multiply(new BigDecimal("20")))
                        .setScale(1, RoundingMode.HALF_UP);
        CarrierRating item = new CarrierRating();
        item.setCarrierCode(carrier.getCode());
        item.setCarrierName(carrier.getName());
        item.setPeriod(period);
        item.setWaybillCount(count);
        item.setClosedCount(closed);
        item.setOnTimeCount(onTime);
        item.setOnTimeRate(onTimeRate);
        item.setExceptionCount(exceptions);
        item.setExceptionRate(exceptionRate);
        item.setPodReturnRate(podRate);
        item.setAvgTransitHours(
                transitCount == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(transit / transitCount).setScale(1, RoundingMode.HALF_UP));
        item.setScore(score);
        item.setGrade(score.compareTo(new BigDecimal("90")) >= 0 ? "A"
                : score.compareTo(new BigDecimal("75")) >= 0 ? "B"
                : score.compareTo(new BigDecimal("60")) >= 0 ? "C" : "D");
        item.setComputedAt(LocalDateTime.now());
        return item;
    }

    private BigDecimal ratio(int numerator, int denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 3, RoundingMode.HALF_UP);
    }
}
