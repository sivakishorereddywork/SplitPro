package com.splitpro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitpro.dto.ExpenseDTOs;
import com.splitpro.model.Expense;
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
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final FriendService friendService;

    @Transactional
    public Expense createExpense(String payerId, ExpenseDTOs.CreateExpenseRequest request) {
        log.info("Creating expense: {} by user: {}", request.getDescription(), payerId);
        
        // Validate payer exists
        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new IllegalArgumentException("Payer not found"));
        
        // Validate group if specified
        Group group = null;
        if (request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Group not found"));
            
            if (!group.isMember(payerId)) {
                throw new IllegalArgumentException("Payer is not a member of the specified group");
            }
        }
        
        // Validate all participants exist
        List<String> participantIds = request.getSplits().stream()
                .map(ExpenseDTOs.SplitRequest::getUserId)
                .collect(Collectors.toList());
        
        Map<String, User> participants = userRepository.findAllById(participantIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        
        if (participants.size() != participantIds.size()) {
            throw new IllegalArgumentException("Some participants not found");
        }
        
        // Calculate splits
        List<Expense.ExpenseSplit> calculatedSplits = calculateSplits(
                request.getSplits(), 
                request.getTotalAmount(), 
                participants
        );
        
        // Create expense
        Expense expense = Expense.builder()
                .description(request.getDescription())
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency())
                .payerId(payerId)
                .payerName(payer.getName())
                .groupId(request.getGroupId())
                .groupName(group != null ? group.getName() : null)
                .splits(calculatedSplits)
                .createdAt(LocalDateTime.now())
                .occurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : LocalDateTime.now())
                .category(request.getCategory() != null ? request.getCategory() : Expense.ExpenseCategory.GENERAL)
                .notes(request.getNotes())
                .active(true)
                .build();
        
        Expense savedExpense = expenseRepository.save(expense);
        
        // Update friend balances
        updateBalancesAfterExpense(savedExpense);
        
        log.info("Expense created: {} with {} splits", savedExpense.getId(), calculatedSplits.size());
        return savedExpense;
    }

    private List<Expense.ExpenseSplit> calculateSplits(
            List<ExpenseDTOs.SplitRequest> splitRequests,
            BigDecimal totalAmount,
            Map<String, User> participants) {
        
        List<Expense.ExpenseSplit> splits = new ArrayList<>();
        
        // Group splits by type for easier calculation
        Map<Expense.SplitType, List<ExpenseDTOs.SplitRequest>> splitsByType = 
                splitRequests.stream().collect(Collectors.groupingBy(ExpenseDTOs.SplitRequest::getSplitType));
        
        BigDecimal remainingAmount = totalAmount;
        
        // Handle AMOUNT splits first (fixed amounts)
        if (splitsByType.containsKey(Expense.SplitType.AMOUNT)) {
            for (ExpenseDTOs.SplitRequest split : splitsByType.get(Expense.SplitType.AMOUNT)) {
                BigDecimal amount = split.getSplitValue();
                if (amount.compareTo(remainingAmount) > 0) {
                    throw new IllegalArgumentException("Fixed amount splits exceed total expense amount");
                }
                
                User participant = participants.get(split.getUserId());
                splits.add(Expense.ExpenseSplit.builder()
                        .userId(split.getUserId())
                        .userName(participant.getName())
                        .splitType(Expense.SplitType.AMOUNT)
                        .splitValue(amount)
                        .amountOwed(amount)
                        .build());
                
                remainingAmount = remainingAmount.subtract(amount);
            }
        }
        
        // Handle PERCENT splits
        if (splitsByType.containsKey(Expense.SplitType.PERCENT)) {
            BigDecimal totalPercentage = splitsByType.get(Expense.SplitType.PERCENT)
                    .stream()
                    .map(ExpenseDTOs.SplitRequest::getSplitValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Percentage splits cannot exceed 100%");
            }
            
            for (ExpenseDTOs.SplitRequest split : splitsByType.get(Expense.SplitType.PERCENT)) {
                BigDecimal percentage = split.getSplitValue();
                BigDecimal amount = totalAmount.multiply(percentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                
                User participant = participants.get(split.getUserId());
                splits.add(Expense.ExpenseSplit.builder()
                        .userId(split.getUserId())
                        .userName(participant.getName())
                        .splitType(Expense.SplitType.PERCENT)
                        .splitValue(percentage)
                        .amountOwed(amount)
                        .build());
            }
        }
        
        // Handle EQUAL splits
        if (splitsByType.containsKey(Expense.SplitType.EQUAL)) {
            List<ExpenseDTOs.SplitRequest> equalSplits = splitsByType.get(Expense.SplitType.EQUAL);
            BigDecimal equalAmount = remainingAmount.divide(
                    BigDecimal.valueOf(equalSplits.size()), 
                    2, 
                    RoundingMode.HALF_UP
            );
            
            for (ExpenseDTOs.SplitRequest split : equalSplits) {
                User participant = participants.get(split.getUserId());
                splits.add(Expense.ExpenseSplit.builder()
                        .userId(split.getUserId())
                        .userName(participant.getName())
                        .splitType(Expense.SplitType.EQUAL)
                        .splitValue(equalAmount)
                        .amountOwed(equalAmount)
                        .build());
            }
        }
        
        return splits;
    }

    private void updateBalancesAfterExpense(Expense expense) {
        String payerId = expense.getPayerId();
        
        for (Expense.ExpenseSplit split : expense.getSplits()) {
            if (!split.getUserId().equals(payerId)) {
                // Participant owes money to the payer
                friendService.updateBalance(payerId, split.getUserId(), split.getAmountOwed());
            }
        }
    }

    public Page<ExpenseDTOs.ExpenseResponse> getUserExpenses(String userId, Pageable pageable) {
        Page<Expense> expenses = expenseRepository.findByUserInvolvement(userId, pageable);
        return expenses.map(this::toExpenseResponse);
    }

    public List<ExpenseDTOs.ExpenseResponse> getGroupExpenses(String groupId, String userId) {
        // Verify user is member of the group
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        
        if (!group.isMember(userId)) {
            throw new IllegalArgumentException("Access denied: Not a group member");
        }
        
        return expenseRepository.findByGroupIdAndActiveTrue(groupId)
                .stream()
                .map(this::toExpenseResponse)
                .collect(Collectors.toList());
    }

    public ExpenseDTOs.ExpenseResponse getExpenseDetails(String expenseId, String userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .filter(Expense::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        
        // Check if user is involved in this expense
        boolean isInvolved = expense.getPayerId().equals(userId) ||
                expense.getSplits().stream().anyMatch(split -> split.getUserId().equals(userId));
        
        if (!isInvolved) {
            throw new IllegalArgumentException("Access denied: Not involved in this expense");
        }
        
        return toExpenseResponse(expense);
    }

    @Transactional
    public void deleteExpense(String expenseId, String userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .filter(Expense::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        
        if (!expense.getPayerId().equals(userId)) {
            throw new IllegalArgumentException("Access denied: Only the payer can delete an expense");
        }
        
        // Reverse balance updates
        reverseBalancesAfterExpense(expense);
        
        // Mark as deleted
        expense.setActive(false);
        expenseRepository.save(expense);
        
        log.info("Expense deleted: {} by user: {}", expenseId, userId);
    }

    private void reverseBalancesAfterExpense(Expense expense) {
        String payerId = expense.getPayerId();
        
        for (Expense.ExpenseSplit split : expense.getSplits()) {
            if (!split.getUserId().equals(payerId)) {
                // Reverse the balance update
                friendService.updateBalance(payerId, split.getUserId(), split.getAmountOwed().negate());
            }
        }
    }

    public ExpenseDTOs.ExpenseResponse toExpenseResponse(Expense expense) {
        List<ExpenseDTOs.SplitResponse> splitResponses = expense.getSplits()
                .stream()
                .map(split -> ExpenseDTOs.SplitResponse.builder()
                        .userId(split.getUserId())
                        .userName(split.getUserName())
                        .splitType(split.getSplitType())
                        .splitValue(split.getSplitValue())
                        .amountOwed(split.getAmountOwed())
                        .build())
                .collect(Collectors.toList());
        
        return ExpenseDTOs.ExpenseResponse.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .totalAmount(expense.getTotalAmount())
                .currency(expense.getCurrency())
                .payerId(expense.getPayerId())
                .payerName(expense.getPayerName())
                .groupId(expense.getGroupId())
                .groupName(expense.getGroupName())
                .splits(splitResponses)
                .createdAt(expense.getCreatedAt())
                .occurredAt(expense.getOccurredAt())
                .category(expense.getCategory())
                .notes(expense.getNotes())
                .isBalanced(expense.isBalanced())
                .build();
    }
}