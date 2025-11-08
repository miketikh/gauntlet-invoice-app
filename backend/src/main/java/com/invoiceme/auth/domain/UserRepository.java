package com.invoiceme.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);
}