package com.splitpro.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splitpro.dto.ExpenseDTOs;
import com.splitpro.service.BalanceService;
import com.splitpro.util.SecurityUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<ExpenseDTOs.BalanceResponse> getUserBalances(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        ExpenseDTOs.BalanceResponse balances = balanceService.getUserBalances(userId);
        return ResponseEntity.ok(balances);
    }

    private String getCurrentUserId(Authentication authentication) {
        return securityUtils.getCurrentUserId(authentication); // Update this method
    }
}