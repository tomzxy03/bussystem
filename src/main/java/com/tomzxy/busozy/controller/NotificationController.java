package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.common.enums.NotificationStatus;
import com.tomzxy.busozy.dto.response.NotificationResDTO;
import com.tomzxy.busozy.dto.response.UnreadCountResDTO;
import com.tomzxy.busozy.service.interfaces.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Tag(name = "Notifications", description = "Notification inbox APIs")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Danh sách thông báo của tôi")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<NotificationResDTO>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getMyNotifications(
                        extractUserId(userDetails),
                        parseStatuses(status),
                        PageRequestFactory.build(page, size, "createdAt,desc", Sort.Direction.DESC))));
    }

    @Operation(summary = "Số lượng thông báo chưa đọc")
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<UnreadCountResDTO>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getUnreadCount(extractUserId(userDetails))));
    }

    @Operation(summary = "Đánh dấu một thông báo là đã đọc")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResDTO>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.markAsRead(extractUserId(userDetails), id)));
    }

    @Operation(summary = "Đánh dấu tất cả thông báo là đã đọc")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(extractUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã đánh dấu tất cả là đã đọc"));
    }

    private Collection<NotificationStatus> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return Arrays.stream(status.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toUpperCase)
                .map(NotificationStatus::valueOf)
                .collect(Collectors.toSet());
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof com.tomzxy.busozy.entity.User user) {
            return user.getId();
        }
        throw new com.tomzxy.busozy.exception.UnauthorizedException(
                com.tomzxy.busozy.common.enums.ErrorCode.ACCESS_DENIED);
    }
}
