package com.tms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_customer")
public class Customer extends BaseEntity {
    private String code, name, contact, phone, province, city, address;
    private BigDecimal lng, lat;
    private String apiKey;
    private String callbackUrl;
    private Boolean pushEnabled;
}
