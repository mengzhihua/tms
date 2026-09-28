package com.tms.selection.controller;

import com.tms.common.BaseCrudController;
import com.tms.selection.entity.SelectionRule;
import com.tms.selection.mapper.SelectionRuleMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/selection-rule")
public class SelectionRuleController
        extends BaseCrudController<SelectionRule, SelectionRuleMapper> {
    public SelectionRuleController() {
        super(SelectionRule.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[] {"code", "name", "strategy"};
    }
}
