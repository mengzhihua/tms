package com.tms.basic.controller;

import com.tms.basic.entity.CarrierCoverage;
import com.tms.basic.mapper.CarrierCoverageMapper;
import com.tms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/carrier-coverage")
public class CarrierCoverageController
        extends BaseCrudController<CarrierCoverage, CarrierCoverageMapper> {
    public CarrierCoverageController() {
        super(CarrierCoverage.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[] {"carrier_code", "region_code", "service_level_code"};
    }
}
