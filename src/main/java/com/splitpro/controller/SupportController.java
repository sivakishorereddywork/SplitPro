package com.splitpro.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    @PostMapping
    public ResponseEntity<SupportResponse> submitSupportRequest(
            @Valid @RequestBody SupportRequest supportRequest) {
        
        log.info("Support request received from: {} ({})", 
                supportRequest.getName(), supportRequest.getEmail());
        
        try {
            log.info("Support request details: {}", supportRequest);
            
            String ticketId = generateTicketId();
            
            SupportResponse response = SupportResponse.builder()
                .message("Thank you for contacting Split PRO support! We've received your message and will get back to you within 24 hours.")
                .ticketId(ticketId)
                .estimatedResponseTime("24 hours")
                .timestamp(LocalDateTime.now())
                .build();
            
            log.info("Support ticket created: {}", ticketId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing support request", e);
            
            SupportResponse errorResponse = SupportResponse.builder()
                .message("We're experiencing technical difficulties. Please try again later or email support@splitpro.com directly.")
                .timestamp(LocalDateTime.now())
                .build();
                
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private String generateTicketId() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        return String.format("SP-%d-%03d", timestamp % 1000000, random);
    }

    @Data
    public static class SupportRequest {
        
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @Email(message = "Please provide a valid email address")
        @NotBlank(message = "Email is required")
        private String email;

        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        private String phone;

        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters")
        private String message;
    }

    @Data
    @Builder
    public static class SupportResponse {
        private String message;
        private String ticketId;
        private String estimatedResponseTime;
        private LocalDateTime timestamp;
    }
}