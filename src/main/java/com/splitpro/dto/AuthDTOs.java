package com.splitpro.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Objects for Authentication API
 */
public class AuthDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignupRequest {
        
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @Email(message = "Please provide a valid email address")
        private String email;

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid phone number")
        private String phone;

        @NotBlank(message = "Password is required")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{7,}$",
            message = "Password must be at least 7 characters and contain at least one uppercase letter, one lowercase letter, one number, and one special character"
        )
        private String password;

        @NotBlank(message = "Password confirmation is required")
        private String passwordConfirm;

        public boolean isValid() {
            boolean hasIdentifier = (email != null && !email.trim().isEmpty()) || 
                                  (phone != null && !phone.trim().isEmpty());
            boolean passwordsMatch = password != null && password.equals(passwordConfirm);
            return hasIdentifier && passwordsMatch;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        
        @NotBlank(message = "Email or phone is required")
        private String identifier;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        
        private String id;
        private String name;
        private String email;
        private String phone;
        private boolean emailVerified;
        private boolean phoneVerified;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        private boolean accountLocked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        
        private UserResponse user;
        private String message;
        private long expiresIn;

        public static AuthResponse success(UserResponse user, String message, long expiresIn) {
            return AuthResponse.builder()
                .user(user)
                .message(message)
                .expiresIn(expiresIn)
                .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForgotPasswordRequest {
        
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        
        private String message;
        private int status;
        private String path;
        private LocalDateTime timestamp;
        private java.util.Map<String, String> validationErrors;

        public static ErrorResponse of(String message, int status, String path) {
            return ErrorResponse.builder()
                .message(message)
                .status(status)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
        }
    }
}