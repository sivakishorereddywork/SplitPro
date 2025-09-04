package com.splitpro.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtService {

    @Value("${splitpro.security.jwt.secret}")
    private String jwtSecret;

    @Value("${splitpro.security.jwt.access-token-expiry:900000}")
    private long accessTokenExpiry;

    @Value("${splitpro.security.jwt.refresh-token-expiry:604800000}")
    private long refreshTokenExpiry;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        try {
            // Ensure the secret is long enough for HS256
            if (jwtSecret == null || jwtSecret.length() < 32) {
                throw new IllegalArgumentException("JWT secret must be at least 32 characters long for HS256");
            }
            
            this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            log.info("JWT Service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize JWT Service: {}", e.getMessage());
            throw new RuntimeException("JWT Service initialization failed", e);
        }
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    public String extractUserId(String token) {
        try {
            return extractClaim(token, claims -> claims.get("userId", String.class));
        } catch (Exception e) {
            log.warn("Failed to extract userId from token: {}", e.getMessage());
            return null;
        }
    }

    public String extractTokenType(String token) {
        try {
            return extractClaim(token, claims -> claims.get("type", String.class));
        } catch (Exception e) {
            log.warn("Failed to extract token type: {}", e.getMessage());
            return null;
        }
    }

    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            log.warn("Failed to extract expiration from token: {}", e.getMessage());
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            log.warn("Failed to extract claim from token: {}", e.getMessage());
            return null;
        }
    }

    public String generateAccessToken(UserDetails userDetails, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "access");
        claims.put("authorities", userDetails.getAuthorities());
        
        return createToken(claims, userDetails.getUsername(), accessTokenExpiry);
    }

    public String generateRefreshToken(UserDetails userDetails, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        claims.put("tokenVersion", UUID.randomUUID().toString());
        
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiry);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);

            return Jwts.builder()
                    .setClaims(claims)  // FIXED: use setClaims instead of claims
                    .setSubject(subject)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .setIssuer("Split PRO")
                    .setId(UUID.randomUUID().toString())
                    .signWith(getSigningKey())
                    .compact();
        } catch (Exception e) {
            log.error("Failed to create JWT token: {}", e.getMessage());
            throw new RuntimeException("Token generation failed", e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            log.warn("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            return "access".equals(extractTokenType(token));
        } catch (Exception e) {
            log.warn("Failed to check if token is access token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractTokenType(token));
        } catch (Exception e) {
            log.warn("Failed to check if token is refresh token: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    public long getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

    public String extractTokenVersion(String refreshToken) {
        try {
            return extractClaim(refreshToken, claims -> claims.get("tokenVersion", String.class));
        } catch (Exception e) {
            log.warn("Failed to extract token version: {}", e.getMessage());
            return null;
        }
    }
}