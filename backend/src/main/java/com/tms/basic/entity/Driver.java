package com.tms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_driver")
public class Driver extends BaseEntity {
    private String code, name, phone, idCard, licenseType, carrierCode, status;
}
