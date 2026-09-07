package com.tms.basic.controller;

import com.tms.basic.entity.Customer;
import com.tms.basic.mapper.CustomerMapper;
import com.tms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/customer")
public class CustomerController extends BaseCrudController<Customer, CustomerMapper> {
    public CustomerController() {
        super(Customer.class);
    }

    protected String[] keywordColumns() {
        return new String[] {"code", "name", "city"};
    }
}
