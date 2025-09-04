package com.splitpro.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.splitpro.dto.ExpenseDTOs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final FriendService friendService;

    public ExpenseDTOs.BalanceResponse getUserBalances(String userId) {
        List<ExpenseDTOs.FriendResponse> friends = friendService.getUserFriends(userId);
        
        Map<String, BigDecimal> friendBalances = new HashMap<>();
        Map<String, BigDecimal> groupBalances = new HashMap<>(); // TODO: Implement group balances
        
        BigDecimal totalOwed = BigDecimal.ZERO;
        BigDecimal totalOwedToYou = BigDecimal.ZERO;
        
        for (ExpenseDTOs.FriendResponse friend : friends) {
            friendBalances.put(friend.getFriendId(), friend.getBalance());
            
            if (friend.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                // Friend owes you money
                totalOwedToYou = totalOwedToYou.add(friend.getBalance());
            } else if (friend.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                // You owe friend money
                totalOwed = totalOwed.add(friend.getBalance().abs());
            }
        }
        
        BigDecimal netBalance = totalOwedToYou.subtract(totalOwed);
        
        return ExpenseDTOs.BalanceResponse.builder()
                .friendBalances(friendBalances)
                .groupBalances(groupBalances)
                .totalOwed(totalOwed)
                .totalOwedToYou(totalOwedToYou)
                .netBalance(netBalance)
                .build();
    }
}