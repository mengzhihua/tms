package com.tms.basic.controller;

import com.tms.basic.entity.ServiceLevel;
import com.tms.basic.mapper.ServiceLevelMapper;
import com.tms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/service-level")
public class ServiceLevelController extends BaseCrudController<ServiceLevel, ServiceLevelMapper> {
    public ServiceLevelController() {
        super(ServiceLevel.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[] {"code", "name"};
    }
}
