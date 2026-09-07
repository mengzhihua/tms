package com.tms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_geofence")
public class Geofence extends BaseEntity {
    private String code, name, type, siteCode, polygon, status;
    private BigDecimal centerLng, centerLat, radiusM;
    private Boolean alertOnEnter, alertOnExit;
}
