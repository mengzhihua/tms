package com.tms.basic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tms.basic.entity.Region;
import com.tms.basic.mapper.RegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegionService {
    private final RegionMapper mapper;

    public String match(String province, String city) {
        if (city != null) {
            Region result =
                    mapper.selectOne(
                            new LambdaQueryWrapper<Region>()
                                    .eq(Region::getStatus, "ENABLED")
                                    .like(Region::getCities, city)
                                    .last("LIMIT 1"));
            if (result != null) {
                return result.getCode();
            }
        }
        if (province != null) {
            Region result =
                    mapper.selectOne(
                            new LambdaQueryWrapper<Region>()
                                    .eq(Region::getStatus, "ENABLED")
                                    .like(Region::getProvinces, province)
                                    .last("LIMIT 1"));
            if (result != null) {
                return result.getCode();
            }
        }
        return null;
    }
}
