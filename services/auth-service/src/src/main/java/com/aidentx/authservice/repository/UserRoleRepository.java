package com.aidentx.authservice.repository;

import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing {@link UserRole} entities.
 * <p>
 * Extends {@link JpaRepository} to provide CRUD operations on UserRole entities
 * with primary keys of type {@link UUID}.
 */
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /**
     * Retrieves all {@link UserRole} entities associated with a given {@link User}.
     *
     * @param user the {@link User} entity to find roles for
     * @return a list of {@link UserRole} entities linked to the specified user,
     * or an empty list if none found
     */
    List<UserRole> findByUser(User user);
}
