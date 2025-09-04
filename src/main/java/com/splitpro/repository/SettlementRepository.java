package com.splitpro.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.splitpro.model.Settlement;

@Repository
public interface SettlementRepository extends MongoRepository<Settlement, String> {

    List<Settlement> findByFromUserIdAndActiveTrue(String fromUserId);

    List<Settlement> findByToUserIdAndActiveTrue(String toUserId);

    @Query("{ $or: [ { 'fromUserId': ?0 }, { 'toUserId': ?0 } ], 'active': true }")
    Page<Settlement> findByUserInvolvement(String userId, Pageable pageable);

    List<Settlement> findByGroupIdAndActiveTrue(String groupId);

    @Query("{ 'fromUserId': ?0, 'toUserId': ?1, 'active': true }")
    List<Settlement> findBetweenUsers(String fromUserId, String toUserId);

    long countByFromUserId(String fromUserId);
}