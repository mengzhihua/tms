package com.tms.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_transport_order")
public class TransportOrder extends BaseEntity {
    private String code,
            sourceNo,
            customerCode,
            orderType,
            fromSiteCode,
            consignorName,
            consignorPhone,
            consignorAddress,
            consigneeName,
            consigneePhone,
            consigneeAddress,
            consigneeProvince,
            consigneeCity,
            serviceLevelCode,
            regionCode,
            carrierCode,
            recommendRemark,
            originOrderCode,
            status,
            waybillCode,
            signer,
            podImage,
            podRemark;
    private BigDecimal consignorLng,
            consignorLat,
            consigneeLng,
            consigneeLat,
            totalQty,
            totalWeightKg,
            totalVolumeM3,
            volumetricWeightKg,
            chargeableWeightKg,
            volumeRatio;
    private LocalDateTime requiredDeliveryTime, signTime, promisedArriveTime;
    private Long waybillId;
    private Integer priority;

    @TableField(exist = false)
    private List<TransportOrderLine> lines;
}
