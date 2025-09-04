package com.splitpro.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitpro.dto.ExpenseDTOs;
import com.splitpro.model.Friend;
import com.splitpro.model.User;
import com.splitpro.repository.FriendRepository;
import com.splitpro.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    @Transactional
    public Friend addFriend(String userId, String friendIdentifier) {
        log.info("Adding friend: {} for user: {}", friendIdentifier, userId);
        
        // Find the friend user by email or phone
        User friendUser = userRepository.findByEmailOrPhone(friendIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found with identifier: " + friendIdentifier));
        
        if (friendUser.getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot add yourself as a friend");
        }
        
        // Check if friendship already exists
        if (friendRepository.existsByUserIdAndFriendIdAndActiveTrue(userId, friendUser.getId())) {
            throw new IllegalArgumentException("User is already your friend");
        }
        
        // Create bidirectional friendship
        Friend friendship1 = Friend.builder()
                .userId(userId)
                .friendId(friendUser.getId())
                .friendName(friendUser.getName())
                .friendEmail(friendUser.getEmail())
                .balance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();
        
        Friend friendship2 = Friend.builder()
                .userId(friendUser.getId())
                .friendId(userId)
                .friendName(getUserName(userId))
                .friendEmail(getUserEmail(userId))
                .balance(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();
        
        friendRepository.save(friendship1);
        friendRepository.save(friendship2);
        
        log.info("Friendship created between {} and {}", userId, friendUser.getId());
        return friendship1;
    }

    public List<ExpenseDTOs.FriendResponse> getUserFriends(String userId) {
        return friendRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(this::toFriendResponse)
                .collect(Collectors.toList());
    }

    public Optional<Friend> getFriendship(String userId, String friendId) {
        return friendRepository.findByUserIdAndFriendIdAndActiveTrue(userId, friendId);
    }

    @Transactional
    public void updateBalance(String userId, String friendId, BigDecimal amount) {
        Optional<Friend> friendshipOpt = friendRepository.findByUserIdAndFriendIdAndActiveTrue(userId, friendId);
        Optional<Friend> reverseFriendshipOpt = friendRepository.findByUserIdAndFriendIdAndActiveTrue(friendId, userId);
        
        if (friendshipOpt.isPresent()) {
            Friend friendship = friendshipOpt.get();
            friendship.setBalance(friendship.getBalance().add(amount));
            friendRepository.save(friendship);
        }
        
        if (reverseFriendshipOpt.isPresent()) {
            Friend reverseFriendship = reverseFriendshipOpt.get();
            reverseFriendship.setBalance(reverseFriendship.getBalance().subtract(amount));
            friendRepository.save(reverseFriendship);
        }
    }

    @Transactional
    public void removeFriend(String userId, String friendId) {
        log.info("Removing friendship between {} and {}", userId, friendId);
        
        // Mark both directions as inactive
        friendRepository.findByUserIdAndFriendIdAndActiveTrue(userId, friendId)
                .ifPresent(friendship -> {
                    friendship.setActive(false);
                    friendRepository.save(friendship);
                });
        
        friendRepository.findByUserIdAndFriendIdAndActiveTrue(friendId, userId)
                .ifPresent(friendship -> {
                    friendship.setActive(false);
                    friendRepository.save(friendship);
                });
    }

    public List<ExpenseDTOs.FriendResponse> searchFriends(String userId, String query) {
        return friendRepository.findByUserIdAndFriendNameContaining(userId, query)
                .stream()
                .map(this::toFriendResponse)
                .collect(Collectors.toList());
    }

    private ExpenseDTOs.FriendResponse toFriendResponse(Friend friend) {
        return ExpenseDTOs.FriendResponse.builder()
                .id(friend.getId())
                .friendId(friend.getFriendId())
                .friendName(friend.getFriendName())
                .friendEmail(friend.getFriendEmail())
                .balance(friend.getBalance())
                .createdAt(friend.getCreatedAt())
                .build();
    }

    private String getUserName(String userId) {
        return userRepository.findById(userId)
                .map(User::getName)
                .orElse("Unknown User");
    }

    private String getUserEmail(String userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElse("unknown@example.com");
    }
}