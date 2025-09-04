package com.splitpro.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitpro.dto.ExpenseDTOs;
import com.splitpro.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final UserService userService;

    @GetMapping("/sample-expense")
    public ResponseEntity<ExpenseDTOs.CreateExpenseRequest> getSampleExpense() {
        ExpenseDTOs.CreateExpenseRequest sample = ExpenseDTOs.CreateExpenseRequest.builder()
                .description("Dinner at Italian Restaurant")
                .totalAmount(new BigDecimal("120.00"))
                .currency("USD")
                .splits(Arrays.asList(
                    ExpenseDTOs.SplitRequest.builder()
                        .userId("user1") // This will need to be a real user ID
                        .splitType(com.splitpro.model.Expense.SplitType.EQUAL)
                        .build(),
                    ExpenseDTOs.SplitRequest.builder()
                        .userId("user2") // This will need to be a real user ID
                        .splitType(com.splitpro.model.Expense.SplitType.EQUAL)
                        .build()
                ))
                .category(com.splitpro.model.Expense.ExpenseCategory.FOOD)
                .occurredAt(LocalDateTime.now())
                .build();
        
        return ResponseEntity.ok(sample);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        long totalUsers = userService.getTotalActiveUsers();
        return ResponseEntity.ok(Map.of(
            "totalActiveUsers", totalUsers,
            "timestamp", LocalDateTime.now()
        ));
    }
}