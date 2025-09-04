package com.splitpro.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.splitpro.model.Expense;

@Repository
public interface ExpenseRepository extends MongoRepository<Expense, String> {

    Page<Expense> findByActiveTrue(Pageable pageable);

    List<Expense> findByPayerIdAndActiveTrue(String payerId);

    List<Expense> findByGroupIdAndActiveTrue(String groupId);

    @Query("{ 'splits.userId': ?0, 'active': true }")
    List<Expense> findByParticipantUserId(String userId);

    @Query("{ $or: [ { 'payerId': ?0 }, { 'splits.userId': ?0 } ], 'active': true }")
    Page<Expense> findByUserInvolvement(String userId, Pageable pageable);

    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 }, 'active': true }")
    List<Expense> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByPayerId(String payerId);

    long countByGroupId(String groupId);
}