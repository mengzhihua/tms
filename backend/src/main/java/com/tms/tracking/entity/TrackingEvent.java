package com.tms.tracking.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_tracking_event")
public class TrackingEvent extends BaseEntity {
    private Long waybillId, orderId;
    private String waybillCode, eventType, address, description, source;
    private BigDecimal lng, lat, speedKmh;
    private LocalDateTime eventTime;
}
