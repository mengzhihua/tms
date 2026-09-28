package com.tms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_carrier_coverage")
public class CarrierCoverage extends BaseEntity {
    private String carrierCode;
    private String regionCode;
    private String serviceLevelCode;
    private BigDecimal promisedHours;
    private String status;
}
