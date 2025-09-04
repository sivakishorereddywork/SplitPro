package com.splitpro.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitpro.dto.AuthDTOs;
import com.splitpro.model.User;
import com.splitpro.service.JwtService;
import com.splitpro.service.UserService;
import com.splitpro.util.SecurityUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final SecurityUtils securityUtils;

    @PostMapping("/signup")
    public ResponseEntity<AuthDTOs.AuthResponse> signup(
            @Valid @RequestBody AuthDTOs.SignupRequest signupRequest,
            HttpServletResponse response) {
        
        try {
            log.info("Signup attempt for: {}", signupRequest.getName());
            
            User user = userService.registerUser(signupRequest);
            
            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(user, user.getId());
            String refreshToken = jwtService.generateRefreshToken(user, user.getId());
            
            // Set HTTP-only cookie for access token
            setTokenCookie(response, "split-pro-token", accessToken, 
                          (int) (jwtService.getAccessTokenExpiry() / 1000));
            
            AuthDTOs.UserResponse userResponse = userService.toUserResponse(user);
            AuthDTOs.AuthResponse authResponse = AuthDTOs.AuthResponse.success(
                userResponse, 
                "Account created successfully!", 
                jwtService.getAccessTokenExpiry()
            );
            
            log.info("User registered successfully: {}", user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
            
        } catch (IllegalArgumentException e) {
            log.warn("Signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                AuthDTOs.AuthResponse.builder()
                    .message(e.getMessage())
                    .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during signup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthDTOs.AuthResponse.builder()
                    .message("An unexpected error occurred")
                    .build()
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTOs.AuthResponse> login(
            @Valid @RequestBody AuthDTOs.LoginRequest loginRequest,
            HttpServletResponse response) {
        
        try {
            log.info("Login attempt for: {}", loginRequest.getIdentifier());
            
            User user = userService.authenticateUser(loginRequest);
            
            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(user, user.getId());
            String refreshToken = jwtService.generateRefreshToken(user, user.getId());
            
            // Set HTTP-only cookie for access token
            setTokenCookie(response, "split-pro-token", accessToken, 
                          (int) (jwtService.getAccessTokenExpiry() / 1000));
            
            AuthDTOs.UserResponse userResponse = userService.toUserResponse(user);
            AuthDTOs.AuthResponse authResponse = AuthDTOs.AuthResponse.success(
                userResponse, 
                "Login successful!", 
                jwtService.getAccessTokenExpiry()
            );
            
            log.info("User logged in successfully: {}", user.getId());
            return ResponseEntity.ok(authResponse);
            
        } catch (IllegalArgumentException e) {
            log.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AuthDTOs.AuthResponse.builder()
                    .message("Invalid credentials")
                    .build()
            );
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AuthDTOs.AuthResponse.builder()
                    .message("An unexpected error occurred")
                    .build()
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDTOs.UserResponse> getCurrentUser(Authentication authentication) {
        try {
            String userId = securityUtils.getCurrentUserId(authentication);
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            AuthDTOs.UserResponse userResponse = userService.toUserResponse(user);
            return ResponseEntity.ok(userResponse);
            
        } catch (Exception e) {
            log.warn("Failed to get current user", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // Clear the token cookie
        clearTokenCookie(response, "split-pro-token");
        
        log.info("User logged out successfully");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody AuthDTOs.ForgotPasswordRequest request) {
        try {
            // TODO: Implement forgot password logic
            // For now, just return success message
            log.info("Password reset requested for: {}", request.getEmail());
            return ResponseEntity.ok("Password reset instructions sent to your email!");
        } catch (Exception e) {
            log.error("Error processing forgot password request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process request");
        }
    }

    private void setTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        // cookie.setSameSite("Strict"); // Uncomment if your framework supports it
        response.addCookie(cookie);
    }

    private void clearTokenCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}