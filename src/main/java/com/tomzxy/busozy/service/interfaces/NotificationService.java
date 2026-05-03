package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.common.enums.NotificationChannel;
import com.tomzxy.busozy.common.enums.NotificationStatus;
import com.tomzxy.busozy.dto.response.NotificationResDTO;
import com.tomzxy.busozy.dto.response.UnreadCountResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;

public interface NotificationService {

    NotificationResDTO sendNotification(
            Long userId,
            NotificationChannel channel,
            String title,
            String content,
            Map<String, Object> metadata);

    Page<NotificationResDTO> getMyNotifications(Long userId, Collection<NotificationStatus> statuses, Pageable pageable);

    NotificationResDTO markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    UnreadCountResDTO getUnreadCount(Long userId);

    void retryFailedNotifications();
}
