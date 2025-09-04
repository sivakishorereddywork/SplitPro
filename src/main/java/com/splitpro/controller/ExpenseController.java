package com.splitpro.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.splitpro.model.Expense;
import com.splitpro.service.ExpenseService;
import com.splitpro.util.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ExpenseDTOs.ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseDTOs.CreateExpenseRequest request,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            Expense expense = expenseService.createExpense(userId, request);
            ExpenseDTOs.ExpenseResponse response = expenseService.toExpenseResponse(expense);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create expense for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<ExpenseDTOs.ExpenseResponse>> getUserExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<ExpenseDTOs.ExpenseResponse> expenses = expenseService.getUserExpenses(userId, pageable);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseDTOs.ExpenseResponse> getExpenseDetails(
            @PathVariable String expenseId,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            ExpenseDTOs.ExpenseResponse expense = expenseService.getExpenseDetails(expenseId, userId);
            return ResponseEntity.ok(expense);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<List<ExpenseDTOs.ExpenseResponse>> getGroupExpenses(
            @PathVariable String groupId,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            List<ExpenseDTOs.ExpenseResponse> expenses = expenseService.getGroupExpenses(groupId, userId);
            return ResponseEntity.ok(expenses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable String expenseId,
            Authentication authentication) {
        
        String userId = getCurrentUserId(authentication);
        
        try {
            expenseService.deleteExpense(expenseId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String getCurrentUserId(Authentication authentication) {
        return securityUtils.getCurrentUserId(authentication);
    }
}