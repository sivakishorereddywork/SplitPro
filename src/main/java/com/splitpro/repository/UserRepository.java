package com.splitpro.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.splitpro.model.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    @Query("{ $or: [ { 'email': ?0 }, { 'phone': ?0 } ] }")
    Optional<User> findByEmailOrPhone(String identifier);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByRefreshTokenVersion(String refreshTokenVersion);

    long countByActiveTrue();
}