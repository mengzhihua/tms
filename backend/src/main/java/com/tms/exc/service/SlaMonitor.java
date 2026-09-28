package com.tms.exc.service;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlaMonitor {
    private final ExceptionService exceptionService;

    @Scheduled(fixedDelay = 60000)
    public void scheduledScan() {
        exceptionService.scan(LocalDateTime.now());
    }
}
