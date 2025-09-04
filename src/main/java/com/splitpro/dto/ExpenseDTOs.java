package com.splitpro.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.splitpro.model.Expense;
import com.splitpro.model.Settlement;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ExpenseDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateExpenseRequest {
        
        @NotBlank(message = "Description is required")
        @Size(min = 2, max = 200)
        private String description;
        
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal totalAmount;
        
        @Builder.Default
        private String currency = "USD";
        
        private String groupId; // Optional
        
        @NotEmpty(message = "At least one participant is required")
        private List<SplitRequest> splits;
        
        private Expense.ExpenseCategory category;
        
        private String notes;
        
        private LocalDateTime occurredAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitRequest {
        
        @NotBlank(message = "User ID is required")
        private String userId;
        
        @NotNull(message = "Split type is required")
        private Expense.SplitType splitType;
        
        private BigDecimal splitValue; // For PERCENT or AMOUNT types
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpenseResponse {
        
        private String id;
        private String description;
        private BigDecimal totalAmount;
        private String currency;
        private String payerId;
        private String payerName;
        private String groupId;
        private String groupName;
        private List<SplitResponse> splits;
        private LocalDateTime createdAt;
        private LocalDateTime occurredAt;
        private Expense.ExpenseCategory category;
        private String notes;
        private boolean isBalanced;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitResponse {
        
        private String userId;
        private String userName;
        private Expense.SplitType splitType;
        private BigDecimal splitValue;
        private BigDecimal amountOwed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGroupRequest {
        
        @NotBlank(message = "Group name is required")
        @Size(min = 2, max = 100)
        private String name;
        
        @Size(max = 500)
        private String description;
        
        @NotEmpty(message = "At least one member is required")
        private List<String> memberIds; // User IDs to add to group
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupResponse {
        
        private String id;
        private String name;
        private String description;
        private String createdBy;
        private String createdByName;
        private List<GroupMemberResponse> members;
        private LocalDateTime createdAt;
        private int totalExpenses;
        private BigDecimal totalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMemberResponse {
        
        private String userId;
        private String userName;
        private String userEmail;
        private LocalDateTime joinedAt;
        private BigDecimal balance; // How much this member owes to the group
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendResponse {
        
        private String id;
        private String friendId;
        private String friendName;
        private String friendEmail;
        private BigDecimal balance; // Positive = friend owes you, negative = you owe friend
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddFriendRequest {
        
        @NotBlank(message = "Friend identifier is required")
        private String identifier; // Email or phone number
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceResponse {
        
        private Map<String, BigDecimal> friendBalances; // friendId -> balance
        private Map<String, BigDecimal> groupBalances;  // groupId -> balance
        private BigDecimal totalOwed;   // Total amount you owe others
        private BigDecimal totalOwedToYou; // Total amount others owe you
        private BigDecimal netBalance;  // totalOwedToYou - totalOwed
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateSettlementRequest {
        
        @NotBlank(message = "To user is required")
        private String toUserId;
        
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal amount;
        
        @Builder.Default
        private String currency = "USD";
        
        private String groupId; // Optional
        
        private String description;
        
        @Builder.Default
        private Settlement.SettlementMethod method = Settlement.SettlementMethod.CASH;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementResponse {
        
        private String id;
        private String fromUserId;
        private String fromUserName;
        private String toUserId;
        private String toUserName;
        private BigDecimal amount;
        private String currency;
        private String groupId;
        private String groupName;
        private String description;
        private LocalDateTime settledAt;
        private Settlement.SettlementMethod method;
        private boolean confirmed;
    }
}