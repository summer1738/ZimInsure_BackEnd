package com.ziminsure.insurance.api.config;

import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public JwtAuthenticationFilter(@Lazy UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        logger.info("JwtAuthenticationFilter invoked for URI: {}", request.getRequestURI());
        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("Failed to extract username from JWT: {}", e.getMessage());
            }
        } else if (authHeader != null) {
            logger.warn("Authorization header does not start with Bearer: {}", authHeader);
        } else {
            logger.debug("No Authorization header present");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<User> userOpt = userService.findByEmail(username);
            if (userOpt.isPresent()) {
                if (jwtUtil.validateToken(jwt, username)) {
                    User user = userOpt.get();
                    String role = jwtUtil.extractRole(jwt);
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("JWT authentication successful for user: {} with role: {}", username, role);
                } else {
                    logger.warn("JWT validation failed for user: {}", username);
                }
            } else {
                logger.warn("User not found for username extracted from JWT: {}", username);
            }
        }
        filterChain.doFilter(request, response);
    }
} 