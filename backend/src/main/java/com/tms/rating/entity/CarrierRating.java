package com.tms.rating.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_carrier_rating")
public class CarrierRating extends BaseEntity {
    private String carrierCode;
    private String carrierName;
    private String period;
    private Integer waybillCount;
    private Integer closedCount;
    private Integer onTimeCount;
    private BigDecimal onTimeRate;
    private Integer exceptionCount;
    private BigDecimal exceptionRate;
    private BigDecimal podReturnRate;
    private BigDecimal avgTransitHours;
    private BigDecimal claimAmount;
    private BigDecimal score;
    private String grade;
    private LocalDateTime computedAt;
}
