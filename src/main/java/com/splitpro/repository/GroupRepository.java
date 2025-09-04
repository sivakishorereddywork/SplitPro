package com.splitpro.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.splitpro.model.Group;

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {

    List<Group> findByCreatedByAndActiveTrue(String createdBy);

    @Query("{ 'members.userId': ?0, 'members.active': true, 'active': true }")
    List<Group> findByMemberUserId(String userId);

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'active': true }")
    List<Group> findByNameContainingIgnoreCase(String name);

    long countByCreatedBy(String createdBy);
}