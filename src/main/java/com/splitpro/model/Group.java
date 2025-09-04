package com.splitpro.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "groups")
public class Group {
    
    @Id
    private String id;
    
    @NotBlank(message = "Group name is required")
    @Size(min = 2, max = 100, message = "Group name must be between 2 and 100 characters")
    private String name;
    
    private String description;
    
    private String createdBy; // User ID who created the group
    
    @Builder.Default
    private List<GroupMember> members = new ArrayList<>();
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private boolean active = true;
    
    // Helper methods
    public boolean isMember(String userId) {
        return members.stream().anyMatch(member -> 
            member.getUserId().equals(userId) && member.isActive());
    }
    
    public void addMember(String userId, String userName, String userEmail) {
        GroupMember member = GroupMember.builder()
            .userId(userId)
            .userName(userName)
            .userEmail(userEmail)
            .joinedAt(LocalDateTime.now())
            .active(true)
            .build();
        members.add(member);
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMember {
        private String userId;
        private String userName;
        private String userEmail;
        private LocalDateTime joinedAt;
        @Builder.Default
        private boolean active = true;
    }
}