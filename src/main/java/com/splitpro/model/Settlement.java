package com.splitpro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "settlements")
public class Settlement {
    
    @Id
    private String id;
    
    @NotBlank(message = "From user is required")
    @Indexed
    private String fromUserId; // User who is paying
    
    private String fromUserName; // Cached for display
    
    @NotBlank(message = "To user is required") 
    @Indexed
    private String toUserId; // User who is receiving payment
    
    private String toUserName; // Cached for display
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @Builder.Default
    private String currency = "USD";
    
    @Indexed
    private String groupId; // Optional - null for personal settlements
    
    private String groupName; // Cached for display
    
    private String description; // Optional note about the settlement
    
    @CreatedDate
    private LocalDateTime settledAt;
    
    @Builder.Default
    private SettlementMethod method = SettlementMethod.CASH;
    
    @Builder.Default
    private boolean confirmed = false; // Both parties need to confirm
    
    @Builder.Default
    private boolean active = true;
    
    public enum SettlementMethod {
        CASH("Cash"),
        VENMO("Venmo"),
        PAYPAL("PayPal"),
        ZELLE("Zelle"),
        BANK_TRANSFER("Bank Transfer"),
        OTHER("Other");
        
        private final String displayName;
        
        SettlementMethod(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}