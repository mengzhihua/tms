package com.tms.dispatch.service;

import com.tms.common.CarrierRates;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

/**
 * 按已完成运单给承运商记分。准时 50 分，无异常 30 分，费用贴合 20 分。
 * 费用偏差是实付相对原运费账单的绝对差，换商差额单不计入原报价。
 */
public final class CarrierScore {
    private CarrierScore() {}

    @Getter
    public static final class Trip {
        public final boolean onTime;
        public final boolean exception;
        public final BigDecimal quoted;
        public final BigDecimal actual;

        public Trip(boolean onTime, boolean exception, BigDecimal quoted, BigDecimal actual) {
            this.onTime = onTime;
            this.exception = exception;
            this.quoted = quoted;
            this.actual = actual;
        }
    }

    @Getter
    public static final class Result {
        public final String carrierCode;
        public final int trips;
        public final BigDecimal onTimeRate;
        public final BigDecimal exceptionRate;
        public final BigDecimal costVariance;
        public final BigDecimal score;

        public Result(
                String carrierCode,
                int trips,
                BigDecimal onTimeRate,
                BigDecimal exceptionRate,
                BigDecimal costVariance,
                BigDecimal score) {
            this.carrierCode = carrierCode;
            this.trips = trips;
            this.onTimeRate = onTimeRate;
            this.exceptionRate = exceptionRate;
            this.costVariance = costVariance;
            this.score = score;
        }
    }

    public static Trip trip(
            Boolean exceptionFlag,
            LocalDateTime plannedArrive,
            LocalDateTime actualArrive,
            BigDecimal quoted,
            BigDecimal actual) {
        boolean exception = Boolean.TRUE.equals(exceptionFlag);
        boolean onTime;
        if (plannedArrive == null) {
            onTime = !exception;
        } else if (actualArrive == null) {
            onTime = false;
        } else {
            onTime = !actualArrive.isAfter(plannedArrive);
        }
        return new Trip(onTime, exception, quoted, actual);
    }

    public static Result score(String carrierCode, List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            return new Result(carrierCode, 0, null, null, null, null);
        }
        int onTime = 0;
        int exception = 0;
        BigDecimal varianceSum = BigDecimal.ZERO;
        int varianceCount = 0;
        for (Trip trip : trips) {
            if (trip.onTime) {
                onTime++;
            }
            if (trip.exception) {
                exception++;
            }
            if (trip.quoted != null && trip.quoted.signum() > 0 && trip.actual != null) {
                varianceSum =
                        varianceSum.add(
                                trip.actual
                                        .subtract(trip.quoted)
                                        .abs()
                                        .divide(trip.quoted, 4, RoundingMode.HALF_UP));
                varianceCount++;
            }
        }
        BigDecimal tripsCount = BigDecimal.valueOf(trips.size());
        BigDecimal onTimeRate = BigDecimal.valueOf(onTime).divide(tripsCount, 4, RoundingMode.HALF_UP);
        BigDecimal exceptionRate =
                BigDecimal.valueOf(exception).divide(tripsCount, 4, RoundingMode.HALF_UP);
        BigDecimal costVariance =
                varianceCount == 0
                        ? BigDecimal.ZERO
                        : varianceSum.divide(BigDecimal.valueOf(varianceCount), 4, RoundingMode.HALF_UP);
        BigDecimal costPoints = BigDecimal.ONE.subtract(costVariance.min(BigDecimal.ONE));
        BigDecimal total =
                onTimeRate
                        .multiply(new BigDecimal("50"))
                        .add(BigDecimal.ONE.subtract(exceptionRate).multiply(new BigDecimal("30")))
                        .add(costPoints.multiply(new BigDecimal("20")))
                        .setScale(1, RoundingMode.HALF_UP);
        return new Result(carrierCode, trips.size(), onTimeRate, exceptionRate, costVariance, total);
    }

    /** 分数高者优先。分数相同取费率更低的承运商。没有完成运单时返回 null。 */
    public static Result best(List<Result> results) {
        Result chosen = null;
        if (results == null) {
            return null;
        }
        for (Result result : results) {
            if (result == null || result.score == null) {
                continue;
            }
            if (chosen == null
                    || result.score.compareTo(chosen.score) > 0
                    || (result.score.compareTo(chosen.score) == 0
                            && CarrierRates.rate(result.carrierCode)
                                            .compareTo(CarrierRates.rate(chosen.carrierCode))
                                    < 0)) {
                chosen = result;
            }
        }
        return chosen;
    }

    public static String percent(BigDecimal rate) {
        if (rate == null) {
            return "0%";
        }
        return rate.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
