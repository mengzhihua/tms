package com.tms.basic.controller;

import com.tms.basic.entity.PackageMaterial;
import com.tms.basic.mapper.PackageMaterialMapper;
import com.tms.common.BaseCrudController;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/package-material")
public class PackageMaterialController extends BaseCrudController<PackageMaterial, PackageMaterialMapper> {
    public PackageMaterialController() {
        super(PackageMaterial.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[] {"code", "name"};
    }

    @Override
    protected void beforeSave(PackageMaterial entity) {
        BigDecimal l = entity.getLengthCm() == null ? BigDecimal.ZERO : entity.getLengthCm();
        BigDecimal w = entity.getWidthCm() == null ? BigDecimal.ZERO : entity.getWidthCm();
        BigDecimal h = entity.getHeightCm() == null ? BigDecimal.ZERO : entity.getHeightCm();
        entity.setVolumeM3(l.multiply(w).multiply(h).divide(new BigDecimal("1000000")));
    }
}
