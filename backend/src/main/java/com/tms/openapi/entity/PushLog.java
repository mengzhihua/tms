package com.tms.openapi.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tms.common.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tms_push_log")
public class PushLog extends BaseEntity {
    private String customerCode;
    private String orderCode;
    private String eventType;
    private String url;
    private String payload;
    private Integer responseCode;
    private String responseBody;
    private Boolean success;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String status;
}
