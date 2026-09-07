package com.tms.tracking.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_geofence_state")
public class GeofenceState extends BaseEntity {
    private Long waybillId;
    private String geofenceCode;
    private Boolean inside;
}
