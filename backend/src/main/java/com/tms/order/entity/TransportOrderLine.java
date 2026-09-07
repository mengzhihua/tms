package com.tms.order.entity;
import com.baomidou.mybatisplus.annotation.TableName; import com.tms.common.BaseEntity; import lombok.Data; import lombok.EqualsAndHashCode; import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("tms_transport_order_line")
public class TransportOrderLine extends BaseEntity { private Long orderId; private String itemCode,itemName; private BigDecimal qty,lengthCm,widthCm,heightCm,weightKg,volumeM3,lineWeightKg,lineVolumeM3; }
