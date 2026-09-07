package com.tms.basic.controller;

import com.tms.basic.entity.Route;
import com.tms.basic.mapper.RouteMapper;
import com.tms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/route")
public class RouteController extends BaseCrudController<Route, RouteMapper> {
    public RouteController() {
        super(Route.class);
    }

    protected String[] keywordColumns() {
        return new String[] {"code", "name", "from_site_code", "to_site_code"};
    }
}
