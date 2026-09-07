package com.tms.basic.controller;

import com.tms.basic.entity.Driver;
import com.tms.basic.mapper.DriverMapper;
import com.tms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/driver")
public class DriverController extends BaseCrudController<Driver, DriverMapper> {
    public DriverController() {
        super(Driver.class);
    }

    protected String[] keywordColumns() {
        return new String[] {"code", "name", "phone"};
    }
}
