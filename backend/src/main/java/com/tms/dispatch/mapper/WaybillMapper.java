package com.tms.dispatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tms.dispatch.entity.Waybill;
import org.apache.ibatis.annotations.Select;

public interface WaybillMapper extends BaseMapper<Waybill> {
    @Select("select * from tms_waybill where id = #{id} for update")
    Waybill lockById(Long id);
}
