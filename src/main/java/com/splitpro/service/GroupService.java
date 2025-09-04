package com.splitpro.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitpro.dto.ExpenseDTOs;
import com.splitpro.model.Group;
import com.splitpro.model.User;
import com.splitpro.repository.ExpenseRepository;
import com.splitpro.repository.GroupRepository;
import com.splitpro.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public Group createGroup(String creatorId, ExpenseDTOs.CreateGroupRequest request) {
        log.info("Creating group: {} by user: {}", request.getName(), creatorId);
        
        // Validate all member IDs exist
        for (String memberId : request.getMemberIds()) {
            if (!userRepository.existsById(memberId)) {
                throw new IllegalArgumentException("User not found: " + memberId);
            }
        }
        
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creatorId)
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();
        
        // Add creator as first member
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Creator not found"));
        
        group.addMember(creatorId, creator.getName(), creator.getEmail());
        
        // Add other members
        for (String memberId : request.getMemberIds()) {
            if (!memberId.equals(creatorId)) { // Don't add creator twice
                User member = userRepository.findById(memberId).orElseThrow();
                group.addMember(memberId, member.getName(), member.getEmail());
            }
        }
        
        Group savedGroup = groupRepository.save(group);
        log.info("Group created: {} with {} members", savedGroup.getId(), savedGroup.getMembers().size());
        
        return savedGroup;
    }

    public List<ExpenseDTOs.GroupResponse> getUserGroups(String userId) {
        List<Group> groups = groupRepository.findByMemberUserId(userId);
        return groups.stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

    public Optional<Group> getGroup(String groupId) {
        return groupRepository.findById(groupId).filter(Group::isActive);
    }

    public ExpenseDTOs.GroupResponse getGroupDetails(String groupId, String userId) {
        Group group = getGroup(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        if (!group.isMember(userId)) {
            throw new IllegalArgumentException("Access denied: Not a group member");
        }
        
        return toGroupResponse(group);
    }

    @Transactional
    public Group addMemberToGroup(String groupId, String memberId, String requesterId) {
        Group group = getGroup(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        if (!group.isMember(requesterId)) {
            throw new IllegalArgumentException("Access denied: Not a group member");
        }
        
        if (group.isMember(memberId)) {
            throw new IllegalArgumentException("User is already a group member");
        }
        
        User newMember = userRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        group.addMember(memberId, newMember.getName(), newMember.getEmail());
        
        return groupRepository.save(group);
    }

    @Transactional
    public Group removeMemberFromGroup(String groupId, String memberId, String requesterId) {
        Group group = getGroup(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        if (!group.isMember(requesterId)) {
            throw new IllegalArgumentException("Access denied: Not a group member");
        }
        
        // Only creator or the member themselves can remove
        if (!group.getCreatedBy().equals(requesterId) && !memberId.equals(requesterId)) {
            throw new IllegalArgumentException("Access denied: Cannot remove other members");
        }
        
        group.getMembers().stream()
                .filter(member -> member.getUserId().equals(memberId))
                .findFirst()
                .ifPresent(member -> member.setActive(false));
        
        return groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(String groupId, String requesterId) {
        Group group = getGroup(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        if (!group.getCreatedBy().equals(requesterId)) {
            throw new IllegalArgumentException("Access denied: Only creator can delete group");
        }
        
        group.setActive(false);
        groupRepository.save(group);
        
        log.info("Group deleted: {} by user: {}", groupId, requesterId);
    }

    public List<ExpenseDTOs.GroupResponse> searchGroups(String userId, String query) {
        return groupRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .filter(group -> group.isMember(userId))
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

    public ExpenseDTOs.GroupResponse toGroupResponse(Group group) {
        String creatorName = userRepository.findById(group.getCreatedBy())
                .map(User::getName)
                .orElse("Unknown User");
        
        List<ExpenseDTOs.GroupMemberResponse> memberResponses = group.getMembers()
                .stream()
                .filter(Group.GroupMember::isActive)
                .map(member -> ExpenseDTOs.GroupMemberResponse.builder()
                        .userId(member.getUserId())
                        .userName(member.getUserName())
                        .userEmail(member.getUserEmail())
                        .joinedAt(member.getJoinedAt())
                        .balance(BigDecimal.ZERO) // TODO: Calculate actual balances
                        .build())
                .collect(Collectors.toList());
        
        long expenseCount = expenseRepository.countByGroupId(group.getId());
        
        return ExpenseDTOs.GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdBy(group.getCreatedBy())
                .createdByName(creatorName)
                .members(memberResponses)
                .createdAt(group.getCreatedAt())
                .totalExpenses((int) expenseCount)
                .totalAmount(BigDecimal.ZERO) // TODO: Calculate total amount
                .build();
    }
}