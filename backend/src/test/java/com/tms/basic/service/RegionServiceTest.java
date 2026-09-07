package com.tms.basic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tms.basic.entity.Region;
import com.tms.basic.mapper.RegionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RegionServiceTest {
    @Test
    void cityMatchHasPriorityOverProvinceMatch() {
        RegionMapper mapper = Mockito.mock(RegionMapper.class);
        Region city = new Region();
        city.setCode("EAST");
        Region province = new Region();
        province.setCode("NORTH");
        Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(city, province);

        RegionService service = new RegionService(mapper);

        assertEquals("EAST", service.match("江苏省", "苏州市"));
    }
}
