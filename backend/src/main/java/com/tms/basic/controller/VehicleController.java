package com.tms.basic.controller;

import com.tms.basic.entity.Vehicle;
import com.tms.basic.mapper.VehicleMapper;
import com.tms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/vehicle")
public class VehicleController extends BaseCrudController<Vehicle, VehicleMapper> {
    public VehicleController() {
        super(Vehicle.class);
    }

    protected String[] keywordColumns() {
        return new String[] {"plate_no", "carrier_code", "driver_code"};
    }
}
