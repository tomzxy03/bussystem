package com.tomzxy.busozy.scheduler;

import com.tomzxy.busozy.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryScheduler {

    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 30_000)
    public void retryFailedNotifications() {
        notificationService.retryFailedNotifications();
        log.debug("Notification retry tick completed");
    }
}
