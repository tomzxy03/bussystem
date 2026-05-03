package com.tomzxy.busozy.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.NotificationChannel;
import com.tomzxy.busozy.common.enums.NotificationStatus;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.response.NotificationResDTO;
import com.tomzxy.busozy.dto.response.UnreadCountResDTO;
import com.tomzxy.busozy.entity.Notification;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.ReviewNotificationMapper;
import com.tomzxy.busozy.repository.NotificationRepository;
import com.tomzxy.busozy.repository.UserRepository;
import com.tomzxy.busozy.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_METADATA_BYTES = 2048;
    private static final int MAX_RETRY = 3;
    private static final Duration UNREAD_TTL = Duration.ofMinutes(5);
    private static final String KEY_UNREAD = "notif:unread:";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ReviewNotificationMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public NotificationResDTO sendNotification(
            Long userId,
            NotificationChannel channel,
            String title,
            String content,
            Map<String, Object> metadata) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        validateMetadata(metadata);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setChannel(channel);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setMetadata(metadata);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);

        notificationRepository.save(notification);
        dispatch(notification);
        notificationRepository.save(notification);
        evictUnreadCount(userId);
        return mapper.toNotificationRes(notification);
    }

    @Override
    public Page<NotificationResDTO> getMyNotifications(Long userId, Collection<NotificationStatus> statuses, Pageable pageable) {
        Collection<NotificationStatus> effectiveStatuses = statuses == null || statuses.isEmpty()
                ? EnumSet.of(NotificationStatus.PENDING, NotificationStatus.SENT, NotificationStatus.FAILED)
                : statuses;
        return notificationRepository.findByUserIdAndStatuses(userId, effectiveStatuses, pageable)
                .map(mapper::toNotificationRes);
    }

    @Override
    @Transactional
    public NotificationResDTO markAsRead(Long userId, Long notificationId) {
        int updated = notificationRepository.markAsRead(notificationId, userId, OffsetDateTime.now());
        if (updated == 0) {
            Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND));
            return mapper.toNotificationRes(notification);
        }
        evictUnreadCount(userId);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND));
        return mapper.toNotificationRes(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, OffsetDateTime.now());
        evictUnreadCount(userId);
    }

    @Override
    public UnreadCountResDTO getUnreadCount(Long userId) {
        String key = unreadCacheKey(userId);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof Number number) {
            return new UnreadCountResDTO(number.longValue());
        }
        long count = notificationRepository.countUnreadByUserId(userId);
        redisTemplate.opsForValue().set(key, count, UNREAD_TTL);
        return new UnreadCountResDTO(count);
    }

    @Override
    @Transactional
    public void retryFailedNotifications() {
        for (Notification notification : notificationRepository.findRetryCandidates(MAX_RETRY)) {
            dispatch(notification);
        }
    }

    private void dispatch(Notification notification) {
        try {
            if (notification.getChannel() != NotificationChannel.SYSTEM) {
                throw new BusinessException(ErrorCode.CHANNEL_UNSUPPORTED);
            }
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(OffsetDateTime.now());
        } catch (RuntimeException ex) {
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("Notification dispatch failed: id={}, channel={}, message={}",
                    notification.getId(), notification.getChannel(), ex.getMessage());
        }
    }

    private void validateMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        try {
            if (objectMapper.writeValueAsBytes(metadata).length > MAX_METADATA_BYTES) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Metadata vượt quá 2KB");
            }
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Metadata không hợp lệ");
        }
    }

    private String unreadCacheKey(Long userId) {
        return redisConfig.keyPrefix() + KEY_UNREAD + userId;
    }

    private void evictUnreadCount(Long userId) {
        redisTemplate.delete(unreadCacheKey(userId));
    }
}
