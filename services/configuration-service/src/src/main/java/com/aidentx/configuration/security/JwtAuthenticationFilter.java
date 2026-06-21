package com.aidentx.configuration.security;

import com.aidentx.configuration.audit.AuditPrincipal;
import com.aidentx.configuration.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component(SecurityConstants.JWT_FILTER_BEAN_NAME)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

            if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX) &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());

                if (jwtUtil.validateToken(token)) {
                    // Extract both ID and Username from the token
                    String userId = jwtUtil.extractUserId(token);
                    String username = jwtUtil.extractUsername(token);

                    if (userId != null && username != null) {
                        // Create our custom principal object
                        AuditPrincipal principal = new AuditPrincipal(userId, username);

                        // Create the authentication token with our principal
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                principal, null, new ArrayList<>());

                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // Set the authentication in the security context
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        logger.debug("Security context set for user ID: {}", userId);
                    } else {
                        logger.warn("JWT token valid but missing required claims - userId: {}, username: {}", userId, username);
                    }
                } else {
                    logger.debug("JWT token validation failed");
                }
            } else if (authHeader == null) {
                logger.debug("No Authorization header found in request to {}", request.getRequestURI());
            } else if (!authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
                logger.debug("Authorization header does not start with Bearer prefix");
            } else if (SecurityContextHolder.getContext().getAuthentication() != null) {
                logger.debug("Authentication already set in security context");
            }
        } catch (Exception e) {
            logger.error("JWT filter processing failed", e);
        }

        filterChain.doFilter(request, response);
    }
}