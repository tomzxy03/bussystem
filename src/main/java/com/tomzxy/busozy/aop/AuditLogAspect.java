package com.tomzxy.busozy.aop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomzxy.busozy.common.annotation.Auditable;
import com.tomzxy.busozy.entity.ActivityLog;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.repository.ActivityLogRepository;
import com.tomzxy.busozy.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditable)")
    public Object logAdminAction(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Long entityId = resolveEntityId(joinPoint.getArgs(), auditable.entityIdArgIndex());
        User beforeUser = loadUserSnapshot(auditable.entityType(), entityId);
        Object result = joinPoint.proceed();
        User afterUser = loadUserSnapshot(auditable.entityType(), entityId);

        ActivityLog log = new ActivityLog();
        log.setUser(currentUser());
        log.setAction(auditable.action());
        log.setEntityType(blankToNull(auditable.entityType()));
        log.setEntityId(entityId);
        log.setIpAddress(extractClientIp());
        log.setDetails(buildDetails(joinPoint, auditable, beforeUser, afterUser, result, log.getIpAddress()));
        activityLogRepository.save(log);
        return result;
    }

    private Map<String, Object> buildDetails(
            ProceedingJoinPoint joinPoint,
            Auditable auditable,
            User beforeUser,
            User afterUser,
            Object result,
            String ipAddress) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("action", auditable.action());
        details.put("entityType", blankToNull(auditable.entityType()));
        details.put("entityId", resolveEntityId(joinPoint.getArgs(), auditable.entityIdArgIndex()));
        details.put("method", joinPoint.getSignature().toShortString());
        details.put("ipAddress", ipAddress);
        details.put("userAgent", extractUserAgent());
        details.put("requestPayload", extractRequestPayload(joinPoint.getArgs()));
        details.put("changes", extractChanges(auditable, beforeUser, afterUser));
        if (result != null) {
            details.put("resultType", result.getClass().getSimpleName());
        }
        return details;
    }

    private Map<String, Object> extractRequestPayload(Object[] args) {
        if (args.length <= 1 || args[1] == null) {
            return Map.of();
        }
        return sanitize(objectMapper.convertValue(args[1], new TypeReference<Map<String, Object>>() {
        }));
    }

    private Map<String, Object> extractChanges(Auditable auditable, User beforeUser, User afterUser) {
        if (!"User".equalsIgnoreCase(auditable.entityType()) || beforeUser == null || afterUser == null) {
            return Map.of();
        }

        Map<String, Object> changes = new LinkedHashMap<>();
        putIfChanged(changes, "isBanned", beforeUser.getIsBanned(), afterUser.getIsBanned());
        putIfChanged(changes, "banReason", beforeUser.getBanReason(), afterUser.getBanReason());
        putIfChanged(changes, "status", beforeUser.getStatus(), afterUser.getStatus());
        return changes;
    }

    private void putIfChanged(Map<String, Object> changes, String field, Object oldValue, Object newValue) {
        if (java.util.Objects.equals(oldValue, newValue)) {
            return;
        }
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("old", oldValue);
        diff.put("new", newValue);
        changes.put(field, diff);
    }

    private Map<String, Object> sanitize(Map<String, Object> source) {
        source.remove("password");
        source.remove("passwordHash");
        return source;
    }

    private User loadUserSnapshot(String entityType, Long entityId) {
        if (!"User".equalsIgnoreCase(entityType) || entityId == null) {
            return null;
        }
        return userRepository.findById(entityId)
                .map(this::copyUserSnapshot)
                .orElse(null);
    }

    private User copyUserSnapshot(User user) {
        User snapshot = new User();
        snapshot.setId(user.getId());
        snapshot.setIsBanned(user.getIsBanned());
        snapshot.setBanReason(user.getBanReason());
        snapshot.setStatus(user.getStatus());
        snapshot.setUsername(user.getUsername());
        return snapshot;
    }

    private Long resolveEntityId(Object[] args, int entityIdArgIndex) {
        if (entityIdArgIndex < 0 || entityIdArgIndex >= args.length) {
            return null;
        }
        Object arg = args[entityIdArgIndex];
        if (arg instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private String extractClientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest().getHeader("User-Agent");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
