package com.tms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_vehicle")
public class Vehicle extends BaseEntity {
    private String plateNo, vehicleType, carrierCode, driverCode, status;
    private BigDecimal maxWeightKg, maxVolumeM3, innerLengthCm, innerWidthCm, innerHeightCm, lng, lat;
    private LocalDateTime lastGpsTime;
}
