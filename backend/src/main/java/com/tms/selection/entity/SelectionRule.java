package com.tms.selection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_selection_rule")
public class SelectionRule extends BaseEntity {
    private String code;
    private String name;
    private String strategy;
    private String regionCode;
    private String customerCode;
    private String serviceLevelCode;
    private String designatedCarrier;
    private Integer priority;
    private String status;
}
