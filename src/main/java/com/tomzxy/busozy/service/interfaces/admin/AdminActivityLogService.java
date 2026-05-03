package com.tomzxy.busozy.service.interfaces.admin;

import com.tomzxy.busozy.dto.response.ActivityLogResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminActivityLogService {

    Page<ActivityLogResDTO> getLogs(String action, String entityType, Pageable pageable);
}
