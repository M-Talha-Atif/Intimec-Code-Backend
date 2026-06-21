package com.aidentx.authservice.security;

import com.aidentx.authservice.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that processes JWT tokens from Authorization
 * header.
 * <p>
 * This filter runs once per request. It:
 * - Extracts the JWT token from the Authorization header (if present)
 * - Validates the token
 * - Loads the user details from the database
 * - Sets the authentication in the Spring Security context if the token is
 * valid
 * <p>
 * This enables stateless authentication for protected endpoints using JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Utility for JWT operations (extract, validate, etc.)
    private final JwtUtil jwtUtil;

    // Service to load user details from the database
    private final UserDetailsService userDetailsService;

    /**
     * Constructor for dependency injection.
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil,
            @Qualifier("customUserDetailsService") UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Main filter logic:
     * - Extracts JWT from Authorization header
     * - Validates the token
     * - Loads user details
     * - Sets authentication in the security context if valid
     *
     * @param request     HTTP request
     * @param response    HTTP response
     * @param filterChain Filter chain
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String userId = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);

            try {
                // Extract user ID (UUID string) from the token, not email
                userId = jwtUtil.extractUserId(jwt);
            } catch (Exception e) {
            }
        }

        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userId);

            boolean isValid = jwtUtil.validateToken(jwt);
            if (isValid) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

}