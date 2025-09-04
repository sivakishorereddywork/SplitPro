package com.splitpro.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "friends")
@CompoundIndex(def = "{'userId': 1, 'friendId': 1}", unique = true)
public class Friend {
    
    @Id
    private String id;
    
    private String userId;      // User who added the friend
    private String friendId;    // The friend's user ID
    private String friendName;  // Friend's display name
    private String friendEmail; // Friend's email for reference
    
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO; // How much friend owes to user (positive = owes, negative = owed)
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Builder.Default
    private boolean active = true;
}