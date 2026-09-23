package com.tms.dispatch.service;

import com.tms.common.BizException;
import com.tms.common.CarrierRates;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * 从顺丰、京东、自有车队里按偏好择优。
 * 时效和费率与 {@link CarrierRates} 对齐：顺丰 1 天 / 2.2，京东 2 天 / 1.8，自有车队 3 天 / 1.4。
 */
public final class CarrierAdvisor {
    public static final String FAST = "FAST";
    public static final String CHEAP = "CHEAP";
    public static final String BALANCE = "BALANCE";
    /** 平衡档接受的最长承诺时效。 */
    public static final int BALANCE_MAX_DAYS = 2;

    private CarrierAdvisor() {}

    @Getter
    public static final class Option {
        public final String code;
        public final String name;
        public final BigDecimal rate;
        public final int transitDays;

        public Option(String code, String name, BigDecimal rate, int transitDays) {
            this.code = code;
            this.name = name;
            this.rate = rate;
            this.transitDays = transitDays;
        }
    }

    @Getter
    public static final class Advice {
        public final String preference;
        public final String carrierCode;
        public final String carrierName;
        public final BigDecimal rate;
        public final int transitDays;
        public final String reason;
        public final List<Option> options;

        public Advice(String preference, Option chosen, String reason, List<Option> options) {
            this.preference = preference;
            this.carrierCode = chosen.code;
            this.carrierName = chosen.name;
            this.rate = chosen.rate;
            this.transitDays = chosen.transitDays;
            this.reason = reason;
            this.options = Collections.unmodifiableList(Arrays.asList(options.toArray(new Option[0])));
        }
    }

    public static List<Option> catalog() {
        return Arrays.asList(
                new Option(CarrierRates.SF, "顺丰", CarrierRates.rate(CarrierRates.SF), 1),
                new Option(CarrierRates.JD, "京东", CarrierRates.rate(CarrierRates.JD), 2),
                new Option(CarrierRates.SELF01, "自有车队", CarrierRates.rate(CarrierRates.SELF01), 3));
    }

    public static Advice advise(String preference) {
        String mode = preference == null ? "" : preference.trim().toUpperCase();
        if (mode.isEmpty()) {
            mode = BALANCE;
        }
        if (!FAST.equals(mode) && !CHEAP.equals(mode) && !BALANCE.equals(mode)) {
            throw new BizException("择优方式只支持 FAST、CHEAP、BALANCE");
        }
        List<Option> options = catalog();
        if (FAST.equals(mode)) {
            Option chosen = pickFast(options);
            return new Advice(
                    mode,
                    chosen,
                    "时效优先：选承诺 " + chosen.transitDays + " 天的" + chosen.name + "，费率 " + chosen.rate,
                    options);
        }
        if (CHEAP.equals(mode)) {
            Option chosen = pickCheap(options);
            return new Advice(
                    mode,
                    chosen,
                    "成本优先：选费率 " + chosen.rate + " 的" + chosen.name + "，承诺 " + chosen.transitDays + " 天",
                    options);
        }
        Option chosen = pickBalance(options);
        return new Advice(
                mode,
                chosen,
                "平衡：只在 "
                        + BALANCE_MAX_DAYS
                        + " 天内可达的承运商里选费率更低的"
                        + chosen.name
                        + "，费率 "
                        + chosen.rate,
                options);
    }

    private static Option pickFast(List<Option> options) {
        Option best = null;
        for (Option option : options) {
            if (best == null
                    || option.transitDays < best.transitDays
                    || (option.transitDays == best.transitDays && option.rate.compareTo(best.rate) < 0)) {
                best = option;
            }
        }
        return best;
    }

    private static Option pickCheap(List<Option> options) {
        Option best = null;
        for (Option option : options) {
            if (best == null
                    || option.rate.compareTo(best.rate) < 0
                    || (option.rate.compareTo(best.rate) == 0 && option.transitDays < best.transitDays)) {
                best = option;
            }
        }
        return best;
    }

    private static Option pickBalance(List<Option> options) {
        Option best = null;
        for (Option option : options) {
            if (option.transitDays > BALANCE_MAX_DAYS) {
                continue;
            }
            if (best == null || option.rate.compareTo(best.rate) < 0) {
                best = option;
            }
        }
        if (best == null) {
            return pickFast(options);
        }
        return best;
    }
}
