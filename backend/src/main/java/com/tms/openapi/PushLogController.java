package com.tms.openapi;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tms.common.R;
import com.tms.openapi.entity.PushLog;
import com.tms.openapi.mapper.PushLogMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push-log")
@RequiredArgsConstructor
public class PushLogController {
    private final PushLogMapper mapper;
    private final RoutePushService service;

    @GetMapping("/page")
    public R<Page<PushLog>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(mapper.selectPage(new Page<>(current, size), new QueryWrapper<PushLog>().orderByDesc("id")));
    }

    @PostMapping("/{id}/retry")
    public R<PushLog> retry(@PathVariable Long id) {
        return R.ok(service.retry(id));
    }

    @PostMapping("/mock-receiver")
    public R<Map<String, Object>> mock(@RequestBody(required = false) Map<String, Object> body) {
        return R.ok(body);
    }
}
