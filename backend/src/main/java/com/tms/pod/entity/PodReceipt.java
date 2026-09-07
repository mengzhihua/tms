package com.tms.pod.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_pod_receipt")
public class PodReceipt extends BaseEntity {
    private String code;
    private Long waybillId;
    private String waybillCode;
    private Long orderId;
    private String orderCode;
    private String carrierCode;
    private String receiptType;
    private String status;
    private String signer;
    private LocalDateTime signTime;
    private String imageUrl;
    private LocalDateTime returnedTime;
    private LocalDateTime archivedTime;
}
