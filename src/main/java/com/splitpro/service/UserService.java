package com.splitpro.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitpro.dto.AuthDTOs;
import com.splitpro.model.User;
import com.splitpro.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        return userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional
    public User registerUser(AuthDTOs.SignupRequest signupRequest) {
        log.info("Registering new user: {}", signupRequest.getName());
        
        validateSignupRequest(signupRequest);
        
        if (signupRequest.getEmail() != null && userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        if (signupRequest.getPhone() != null && userRepository.existsByPhone(signupRequest.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        User user = User.builder()
                .name(signupRequest.getName())
                .email(signupRequest.getEmail())
                .phone(signupRequest.getPhone())
                .passwordHash(passwordEncoder.encode(signupRequest.getPassword()))
                .refreshTokenVersion(UUID.randomUUID().toString())
                .active(true)
                .emailVerified(false)
                .phoneVerified(false)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getId());
        
        return savedUser;
    }

    public User authenticateUser(AuthDTOs.LoginRequest loginRequest) {
        log.debug("Authenticating user: {}", loginRequest.getIdentifier());
        
        User user = userRepository.findByEmailOrPhone(loginRequest.getIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!user.isAccountNonLocked()) {
            throw new IllegalArgumentException("Account is temporarily locked due to multiple failed login attempts");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid credentials");
        }

        user.resetFailedLoginAttempts();
        userRepository.save(user);
        
        log.info("User authenticated successfully: {}", user.getId());
        return user;
    }

    @Transactional
    public void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();
        userRepository.save(user);
        
        log.warn("Failed login attempt for user: {}. Attempts: {}", 
                user.getId(), user.getFailedLoginAttempts());
    }

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByIdentifier(String identifier) {
        return userRepository.findByEmailOrPhone(identifier);
    }

    @Transactional
    public void rotateRefreshToken(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshTokenVersion(UUID.randomUUID().toString());
            userRepository.save(user);
            log.debug("Refresh token rotated for user: {}", userId);
        });
    }

    public boolean isValidRefreshTokenVersion(String userId, String tokenVersion) {
        return userRepository.findById(userId)
                .map(user -> tokenVersion.equals(user.getRefreshTokenVersion()))
                .orElse(false);
    }

    public AuthDTOs.UserResponse toUserResponse(User user) {
        return AuthDTOs.UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .accountLocked(user.isAccountLocked())
                .build();
    }

    private void validateSignupRequest(AuthDTOs.SignupRequest request) {
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid signup request");
        }

        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if ((request.getEmail() == null || request.getEmail().trim().isEmpty()) &&
            (request.getPhone() == null || request.getPhone().trim().isEmpty())) {
            throw new IllegalArgumentException("Either email or phone number is required");
        }
    }

    public long getTotalActiveUsers() {
        return userRepository.countByActiveTrue();
    }
}