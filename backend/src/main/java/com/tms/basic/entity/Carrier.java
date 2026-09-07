package com.tms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_carrier")
public class Carrier extends BaseEntity {
    private String code, name, type, contact, phone, apiProvider, apiKey, apiSecret, status;
}
