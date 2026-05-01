package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.common.enums.ErrorCode;
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
        return userRepository.findByEmailOrUsername(credential, credential)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with credential: " + credential));
    }
}
