package com.tms.basic.entity;
import com.baomidou.mybatisplus.annotation.TableName; import com.tms.common.BaseEntity; import lombok.Data; import lombok.EqualsAndHashCode; import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true) @TableName("tms_site")
public class Site extends BaseEntity { private String code,name,type,province,city,address,contact,phone; private BigDecimal lng,lat; }
