package com.splitpro.util;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.splitpro.service.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserService userService;

    public String getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        String username = authentication.getName();
        return userService.findByIdentifier(username)
                .map(user -> user.getId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }
}