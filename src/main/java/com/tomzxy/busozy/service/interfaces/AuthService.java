package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.dto.request.LoginReqDTO;
import com.tomzxy.busozy.dto.request.UpdateProfileReqDTO;
import com.tomzxy.busozy.dto.request.auth.RegisterReqDTO;
import com.tomzxy.busozy.dto.request.auth.RegisterVendorReqDTO;
import com.tomzxy.busozy.dto.response.AuthResDTO;
import com.tomzxy.busozy.dto.response.UserResDTO;

public interface AuthService {

    AuthResDTO register(RegisterReqDTO req, String idempotencyKey);

    AuthResDTO registerVendor(RegisterVendorReqDTO req, String idempotencyKey);

    AuthResDTO login(LoginReqDTO req, String clientIp);

    AuthResDTO refreshToken(String refreshToken);

    void logout(String accessToken);

    UserResDTO getMyProfile(Long userId);

    UserResDTO updateMyProfile(Long userId, UpdateProfileReqDTO req);
}
