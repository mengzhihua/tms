package com.tms.dispatch.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import com.tms.order.entity.TransportOrder;
import com.tms.tracking.entity.TrackingEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_waybill")
public class Waybill extends BaseEntity {
    private String code,
            carrierCode,
            carrierType,
            vehiclePlate,
            driverCode,
            driverName,
            routeCode,
            fromSiteCode,
            thirdPartyNo,
            thirdPartyStatus,
            status,
            exceptionRemark;
    private LocalDateTime plannedDepartTime, plannedArriveTime, actualDepartTime, actualArriveTime;
    private Integer orderCount;
    private BigDecimal totalWeightKg, totalVolumeM3, weightLoadRate, volumeLoadRate, freightAmount;
    private Boolean exceptionFlag;

    @TableField(exist = false)
    private List<TransportOrder> orders;

    @TableField(exist = false)
    private List<TrackingEvent> events;
}
