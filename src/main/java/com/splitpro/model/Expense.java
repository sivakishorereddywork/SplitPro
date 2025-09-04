package com.splitpro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "expenses")
public class Expense {
    
    @Id
    private String id;
    
    @NotBlank(message = "Description is required")
    @Size(min = 2, max = 200, message = "Description must be between 2 and 200 characters")
    private String description;
    
    @NotNull(message = "Total amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal totalAmount;
    
    @Builder.Default
    private String currency = "USD";
    
    @NotBlank(message = "Payer is required")
    @Indexed
    private String payerId; // User ID who paid
    
    private String payerName; // Cached for display
    
    @Indexed
    private String groupId; // Optional - null for personal expenses
    
    private String groupName; // Cached for display
    
    @Builder.Default
    private List<ExpenseSplit> splits = new ArrayList<>();
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    private LocalDateTime occurredAt; // When the expense actually happened
    
    @Builder.Default
    private ExpenseCategory category = ExpenseCategory.GENERAL;
    
    private String notes;
    
    @Builder.Default
    private boolean active = true;
    
    // Helper methods
    public BigDecimal getTotalSplitAmount() {
        return splits.stream()
            .map(ExpenseSplit::getAmountOwed)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public boolean isBalanced() {
        return totalAmount.compareTo(getTotalSplitAmount()) == 0;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseSplit {
        private String userId;
        private String userName; // Cached for display
        private SplitType splitType;
        private BigDecimal splitValue; // Percentage (0-100) or fixed amount
        private BigDecimal amountOwed; // Calculated amount this person owes
    }
    
    public enum SplitType {
        EQUAL,    // Split equally among participants
        PERCENT,  // Split by percentage
        AMOUNT    // Fixed amount per person
    }
    
    public enum ExpenseCategory {
        GENERAL("General"),
        FOOD("Food & Dining"),
        TRANSPORTATION("Transportation"), 
        ENTERTAINMENT("Entertainment"),
        SHOPPING("Shopping"),
        UTILITIES("Utilities"),
        RENT("Rent & Housing"),
        TRAVEL("Travel"),
        OTHER("Other");
        
        private final String displayName;
        
        ExpenseCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}