package com.tms.tracking.entity;
import com.baomidou.mybatisplus.annotation.TableName; import com.tms.common.BaseEntity; import lombok.Data; import lombok.EqualsAndHashCode; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @EqualsAndHashCode(callSuper=true) @TableName("tms_geofence_alert")
public class GeofenceAlert extends BaseEntity { private Long waybillId; private String waybillCode,vehiclePlate,geofenceCode,geofenceName,alertType,handler,handleRemark; private BigDecimal lng,lat; private LocalDateTime alertTime; private Boolean handled; }
