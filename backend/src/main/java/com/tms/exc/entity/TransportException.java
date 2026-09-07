package com.tms.exc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_transport_exception")
public class TransportException extends BaseEntity {
    private String code;
    private Long waybillId;
    private String waybillCode;
    private Long orderId;
    private String orderCode;
    private String carrierCode;
    private String type;
    private String level;
    private String source;
    private String description;
    private String status;
    private String handler;
    private String handleRemark;
    private LocalDateTime handleTime;
    private Boolean claimFlag;
    private BigDecimal claimAmount;
    private String claimStatus;
    private String claimRemark;
}
