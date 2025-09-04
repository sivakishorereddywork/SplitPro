package com.splitpro.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.splitpro.model.Friend;

@Repository
public interface FriendRepository extends MongoRepository<Friend, String> {

    List<Friend> findByUserIdAndActiveTrue(String userId);

    Optional<Friend> findByUserIdAndFriendIdAndActiveTrue(String userId, String friendId);

    @Query("{ $or: [ { 'userId': ?0, 'friendId': ?1 }, { 'userId': ?1, 'friendId': ?0 } ], 'active': true }")
    List<Friend> findBidirectionalFriendship(String userId1, String userId2);

    boolean existsByUserIdAndFriendIdAndActiveTrue(String userId, String friendId);

    long countByUserIdAndActiveTrue(String userId);

    @Query("{ 'userId': ?0, 'friendName': { $regex: ?1, $options: 'i' }, 'active': true }")
    List<Friend> findByUserIdAndFriendNameContaining(String userId, String friendName);
}