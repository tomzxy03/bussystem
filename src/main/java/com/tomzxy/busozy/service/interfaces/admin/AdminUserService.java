package com.tomzxy.busozy.service.interfaces.admin;

import com.tomzxy.busozy.common.enums.UserRole;
import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.dto.request.BanUserReqDTO;
import com.tomzxy.busozy.dto.response.AdminUserResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserResDTO> getUsers(String search, UserStatus status, UserRole role, Boolean banned, Pageable pageable);

    AdminUserResDTO updateBanStatus(Long userId, BanUserReqDTO req);
}
