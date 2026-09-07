package com.tms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_region")
public class Region extends BaseEntity {
    private String code;
    private String name;
    private String level;
    private String provinces;
    private String cities;
    private String status;
}
