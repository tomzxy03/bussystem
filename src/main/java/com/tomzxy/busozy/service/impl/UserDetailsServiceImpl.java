package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads user by email or username (credential field from login).
     */
    @Override
    public UserDetails loadUserByUsername(String credential) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrUsername(credential, credential)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with credential: " + credential));
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED,
                    user.getBanReason() != null && !user.getBanReason().isBlank()
                            ? "Tài khoản đã bị khóa: " + user.getBanReason()
                            : ErrorCode.ACCOUNT_BANNED.getDefaultMessage());
        }
        return user;
    }
}
