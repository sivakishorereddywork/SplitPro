package com.splitpro.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitpro.dto.ExpenseDTOs;
import com.splitpro.model.Group;
import com.splitpro.service.GroupService;
import com.splitpro.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ExpenseDTOs.GroupResponse> createGroup(
            @Valid @RequestBody ExpenseDTOs.CreateGroupRequest request,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            Group group = groupService.createGroup(userId, request);
            ExpenseDTOs.GroupResponse response = groupService.toGroupResponse(group);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create group for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDTOs.GroupResponse>> getUserGroups(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        List<ExpenseDTOs.GroupResponse> groups = groupService.getUserGroups(userId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ExpenseDTOs.GroupResponse> getGroupDetails(
            @PathVariable String groupId,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            ExpenseDTOs.GroupResponse group = groupService.getGroupDetails(groupId, userId);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<ExpenseDTOs.GroupResponse> addMember(
            @PathVariable String groupId,
            @RequestBody AddMemberRequest request,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            Group group = groupService.addMemberToGroup(groupId, request.getUserId(), userId);
            ExpenseDTOs.GroupResponse response = groupService.toGroupResponse(group);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String groupId,
            @PathVariable String memberId,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            groupService.removeMemberFromGroup(groupId, memberId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable String groupId,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            groupService.deleteGroup(groupId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String getCurrentUserId(Authentication authentication) {
        return securityUtils.getCurrentUserId(authentication);
    }

    // Helper DTO for adding members
    public static class AddMemberRequest {
        private String userId;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}