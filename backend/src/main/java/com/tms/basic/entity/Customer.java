package com.tms.basic.entity;
import com.baomidou.mybatisplus.annotation.TableName; import com.tms.common.BaseEntity; import lombok.Data; import lombok.EqualsAndHashCode; import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("tms_customer")
public class Customer extends BaseEntity { private String code,name,contact,phone,province,city,address; private BigDecimal lng,lat; }
