package com.tms.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tms.order.entity.TransportOrder;
import java.util.List;
import org.apache.ibatis.annotations.Select;

public interface TransportOrderMapper extends BaseMapper<TransportOrder> {
    @Select("select * from tms_transport_order where id = #{id} for update")
    TransportOrder lockById(Long id);

    @Select("select * from tms_transport_order where waybill_id = #{waybillId} for update")
    List<TransportOrder> lockByWaybillId(Long waybillId);
}
