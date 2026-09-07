package com.tms.rating.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tms.common.R;
import com.tms.rating.entity.CarrierRating;
import com.tms.rating.mapper.CarrierRatingMapper;
import com.tms.rating.service.CarrierRatingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class RatingController {
    private final CarrierRatingMapper mapper;
    private final CarrierRatingService service;

    @PostMapping("/compute")
    public R<List<CarrierRating>> compute(@RequestParam(required = false) String period) {
        return R.ok(service.compute(period));
    }

    @GetMapping("/page")
    public R<Page<CarrierRating>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String period) {
        return R.ok(
                mapper.selectPage(
                        new Page<>(current, size),
                        new QueryWrapper<CarrierRating>()
                                .eq(period != null, "period", period)
                                .orderByDesc("score")));
    }

    @GetMapping("/rank")
    public R<List<CarrierRating>> rank(@RequestParam(required = false) String period) {
        return R.ok(
                mapper.selectList(
                        new QueryWrapper<CarrierRating>()
                                .eq(period != null, "period", period)
                                .orderByDesc("score")));
    }
}
