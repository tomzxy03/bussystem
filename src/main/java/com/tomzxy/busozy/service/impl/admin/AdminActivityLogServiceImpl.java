package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.dto.response.ActivityLogResDTO;
import com.tomzxy.busozy.entity.ActivityLog;
import com.tomzxy.busozy.repository.ActivityLogRepository;
import com.tomzxy.busozy.service.interfaces.admin.AdminActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminActivityLogServiceImpl implements AdminActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Override
    public Page<ActivityLogResDTO> getLogs(String action, String entityType, Pageable pageable) {
        return activityLogRepository.searchLogs(normalize(action), normalize(entityType), pageable)
                .map(this::toRes);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ActivityLogResDTO toRes(ActivityLog log) {
        return new ActivityLogResDTO(
                log.getId(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getUser() != null ? log.getUser().getUsername() : null,
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getIpAddress(),
                log.getCreatedAt());
    }
}
