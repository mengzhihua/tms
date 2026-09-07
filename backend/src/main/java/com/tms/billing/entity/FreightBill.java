package com.tms.billing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_freight_bill")
public class FreightBill extends BaseEntity {
    private String code,
            waybillCode,
            orderCode,
            carrierCode,
            ruleCode,
            chargeType,
            calcDetail,
            status;
    private Long waybillId, orderId;
    private BigDecimal quantity, amount;
}
