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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.splitpro.dto.ExpenseDTOs;
import com.splitpro.model.Friend;
import com.splitpro.service.FriendService;
import com.splitpro.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ExpenseDTOs.FriendResponse> addFriend(
            @Valid @RequestBody ExpenseDTOs.AddFriendRequest request,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            Friend friend = friendService.addFriend(userId, request.getIdentifier());
            ExpenseDTOs.FriendResponse response = ExpenseDTOs.FriendResponse.builder()
                    .id(friend.getId())
                    .friendId(friend.getFriendId())
                    .friendName(friend.getFriendName())
                    .friendEmail(friend.getFriendEmail())
                    .balance(friend.getBalance())
                    .createdAt(friend.getCreatedAt())
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Failed to add friend for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDTOs.FriendResponse>> getFriends(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        List<ExpenseDTOs.FriendResponse> friends = friendService.getUserFriends(userId);
        return ResponseEntity.ok(friends);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ExpenseDTOs.FriendResponse>> searchFriends(
            @RequestParam String query,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        List<ExpenseDTOs.FriendResponse> friends = friendService.searchFriends(userId, query);
        return ResponseEntity.ok(friends);
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(
            @PathVariable String friendId,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            friendService.removeFriend(userId, friendId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String getCurrentUserId(Authentication authentication) {
        return securityUtils.getCurrentUserId(authentication);
    }
}