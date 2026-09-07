package com.tms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_rate_rule")
public class RateRule extends BaseEntity {
    private String code, name, carrierCode, chargeType, status;
    private String regionCode, serviceLevelCode;
    private BigDecimal firstUnit, firstPrice, addUnit, addPrice, minCharge, volumeRatio;
    private Integer priority;
}
