package com.splitpro.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User implements UserDetails {

    @Id
    private String id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Indexed(unique = true, sparse = true)
    @Email(message = "Please provide a valid email address")
    private String email;

    @Indexed(unique = true, sparse = true)
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid phone number")
    private String phone;

    @NotBlank(message = "Password is required")
    private String passwordHash;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean emailVerified = false;

    @Builder.Default
    private boolean phoneVerified = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String refreshTokenVersion;

    @Builder.Default
    private boolean accountLocked = false;

    private LocalDateTime lastLoginAt;
    
    @Builder.Default
    private int failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;

    // Spring Security UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email != null ? email : phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (accountLocked && lockedUntil != null) {
            if (LocalDateTime.now().isAfter(lockedUntil)) {
                accountLocked = false;
                lockedUntil = null;
                failedLoginAttempts = 0;
                return true;
            }
            return false;
        }
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    // Business methods
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.accountLocked = true;
            this.lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
    }

    public String getIdentifier() {
        return email != null ? email : phone;
    }

    public boolean hasEitherEmailOrPhone() {
        return (email != null && !email.trim().isEmpty()) || 
               (phone != null && !phone.trim().isEmpty());
    }
}