package com.centreformation.controller.api;

import com.centreformation.dto.auth.*;
import com.centreformation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ApiAuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * Authentifie un utilisateur et retourne les tokens JWT
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/register
     * Enregistre un nouvel utilisateur
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh
     * Rafraîchit le token d'accès avec un refresh token (via Header Authorization)
     * Passer le refresh token dans le header, et optionnellement l'ancien access token dans X-Old-Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "X-Old-Token", required = false) String oldAccessToken) {
        String refreshToken = authHeader.replace("Bearer ", "");
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        AuthResponse response = authService.refreshToken(request, oldAccessToken);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Déconnecte l'utilisateur en révoquant ses tokens
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {
        if (userDetails != null) {
            String accessToken = authHeader != null ? authHeader.replace("Bearer ", "") : null;
            authService.logout(userDetails.getUsername(), accessToken);
        }
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

    /**
     * GET /api/auth/me
     * Retourne les informations de l'utilisateur connecté
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }
        
        return ResponseEntity.ok(Map.of(
            "username", userDetails.getUsername(),
            "roles", userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .toList(),
            "authenticated", true
        ));
    }

    /**
     * GET /api/auth/validate
     * Valide si le token est toujours valide (endpoint de test)
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of(
                "valid", false,
                "message", "Token invalide ou expiré"
            ));
        }
        
        return ResponseEntity.ok(Map.of(
            "valid", true,
            "username", userDetails.getUsername(),
            "message", "Token valide"
        ));
    }
}
