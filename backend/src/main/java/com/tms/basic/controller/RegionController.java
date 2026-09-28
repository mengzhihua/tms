package com.tms.basic.controller;

import com.tms.basic.entity.Region;
import com.tms.basic.mapper.RegionMapper;
import com.tms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/region")
public class RegionController extends BaseCrudController<Region, RegionMapper> {
    public RegionController() {
        super(Region.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[] {"code", "name", "provinces", "cities"};
    }
}
