package com.centreformation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, @Lazy UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String requestPath = request.getServletPath();
        
        // Routes publiques - laisser passer sans token
        if (requestPath.equals("/api/auth/login") || 
            requestPath.equals("/api/auth/register") || 
            requestPath.equals("/api/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Pour les routes /api/** (protégées), le token est OBLIGATOIRE
        if (requestPath.startsWith("/api/")) {
            final String authHeader = request.getHeader("Authorization");
            
            // Pas de header Authorization ou pas de Bearer
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendUnauthorizedError(response, "Token JWT manquant");
                return;
            }
            
            final String jwt = authHeader.substring(7);
            
            try {
                final String username = jwtService.extractUsername(jwt);
                
                if (username == null) {
                    sendUnauthorizedError(response, "Token JWT invalide");
                    return;
                }
                
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                    
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("Utilisateur authentifié via JWT: {}", username);
                    } else {
                        sendUnauthorizedError(response, "Token JWT expiré ou invalide");
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la validation du token JWT: {}", e.getMessage());
                sendUnauthorizedError(response, "Token JWT invalide: " + e.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
    
    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
            "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            message,
            java.time.LocalDateTime.now()
        ));
    }
}
