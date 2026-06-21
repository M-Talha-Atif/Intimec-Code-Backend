package com.aidentx.authservice.service;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.entity.UserRole;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.repository.UserRoleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service to load user details for Spring Security authentication.
 * Loads user by UUID and maps roles to authorities from both:
 * - users_role table (legacy auth-service roles: SUPER_ADMIN, USER)
 * - user_role table (new RBAC permissions-service roles: ADMINISTRATOR, REVIEWER, etc.)
 * 
 * This dual approach ensures backward compatibility during migration.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final JdbcTemplate jdbcTemplate;

    public CustomUserDetailsService(
            UserRepository userRepository, 
            UserRoleRepository userRoleRepository,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Loads a user by their UUID string identifier and returns a UserDetails object
     * containing the user's email, hashed password, and granted authorities (roles).
     * 
     * Authorities are gathered from BOTH authentication systems:
     * 1. users_role table (legacy): ROLE_SUPER_ADMIN, ROLE_USER
     * 2. user_role + role tables (new RBAC): ROLE_ADMINISTRATOR, ROLE_REVIEWER, etc.
     *
     * @param id the UUID string identifier of the user
     * @return a UserDetails object with user credentials and authorities
     * @throws UsernameNotFoundException if no user with the given ID is found
     */
    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        // Convert the provided string id to a UUID object
        UUID userId = UUID.fromString(id);

        // Retrieve the user by UUID, or throw exception if not found
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(String.format(AppConstants.USER_NOT_FOUND_BY_ID, id)));

        List<GrantedAuthority> allAuthorities = new ArrayList<>();
        
        // === SOURCE 1: users_role table (legacy auth-service roles) ===
        List<UserRole> authServiceRoles = userRoleRepository.findByUser(user);
        List<GrantedAuthority> legacyAuthorities = authServiceRoles.stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().name()))
                .collect(Collectors.toList());
        allAuthorities.addAll(legacyAuthorities);
        
        // === SOURCE 2: user_role table (new RBAC permissions-service roles) ===
        try {
            String query = "SELECT r.role_type " +
                          "FROM user_role ur " +
                          "JOIN role r ON ur.role_id = r.id " +
                          "WHERE ur.user_id = ? AND ur.tenant_id = ? AND r.is_active = true";
            
            List<String> rbacRoles = jdbcTemplate.queryForList(
                query, 
                String.class, 
                userId.toString(), 
                user.getTenantId()
            );
            
            List<GrantedAuthority> rbacAuthorities = rbacRoles.stream()
                    .map(roleType -> new SimpleGrantedAuthority("ROLE_" + roleType))
                    .collect(Collectors.toList());
            allAuthorities.addAll(rbacAuthorities);
            
        } catch (Exception e) {
            // If user_role query fails (table doesn't exist, etc.), 
            // silently continue with legacy roles only
            // This ensures backward compatibility
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities(allAuthorities)
                .build();
    }
}
