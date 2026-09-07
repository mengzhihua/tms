package com.tms.basic.entity;
import com.baomidou.mybatisplus.annotation.TableName; import com.tms.common.BaseEntity; import lombok.Data; import lombok.EqualsAndHashCode; import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("tms_route")
public class Route extends BaseEntity { private String code,name,fromSiteCode,toSiteCode,viaSites; private BigDecimal distanceKm,estimatedHours; }
