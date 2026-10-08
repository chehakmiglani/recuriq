package com.recuriq.billing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.recuriq.billing.entity.User;
import com.recuriq.billing.enums.UserRole;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Used by login and UserDetailsService lookup.
     * email is the login identity (UNIQUE in DB).
     */
    Optional<User> findByEmail(String email);

    /**
     * Registration guard — check before saving.
     */
    boolean existsByEmail(String email);

    /**
     * Count users who don't have the specified role.
     * Used for staff count (excluding customers).
     */
    long countByRoleNot(UserRole role);

    /**
     * Count users by role.
     * Used for staff count (ADMIN users).
     */
    long countByRole(UserRole role);

}
