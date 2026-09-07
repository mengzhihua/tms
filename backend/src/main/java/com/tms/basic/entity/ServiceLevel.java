package com.tms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_service_level")
public class ServiceLevel extends BaseEntity {
    private String code;
    private String name;
    private BigDecimal promisedHours;
    private Integer priority;
    private String status;
}
