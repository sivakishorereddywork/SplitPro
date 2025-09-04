package com.splitpro.security;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.splitpro.service.JwtService;
import com.splitpro.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    @Value("${splitpro.security.jwt.cookie-name:split-pro-token}")
    private String cookieName;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractTokenFromCookie(request);
            
            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateWithToken(jwt, request);
            }
            
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isEmpty())
                .findFirst()
                .orElse(null);
    }

    private void authenticateWithToken(String jwt, HttpServletRequest request) {
        try {
            String username = jwtService.extractUsername(jwt);
            
            if (username != null) {
                UserDetails userDetails = userService.loadUserByUsername(username);
                
                if (jwtService.isTokenValid(jwt, userDetails) && jwtService.isAccessToken(jwt)) {
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Authentication successful for user: {}", username);
                } else {
                    log.debug("Invalid or expired token for user: {}", username);
                }
            }
        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        
        return path.equals("/api/auth/signup") ||
               path.equals("/api/auth/login") || 
               path.equals("/api/auth/forgot-password") ||
               path.equals("/api/support") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/assets/") ||
               path.equals("/") ||
               path.equals("/index.html") ||
               path.equals("/favicon.ico");
    }
}