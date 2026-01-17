package com.centreformation.service;

import com.centreformation.dto.auth.*;
import com.centreformation.entity.*;
import com.centreformation.exception.AuthenticationException;
import com.centreformation.exception.TokenRefreshException;
import com.centreformation.repository.*;
import com.centreformation.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final EtudiantRepository etudiantRepository;
    private final FormateurRepository formateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Authentifie un utilisateur et retourne les tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Utilisateur utilisateur = utilisateurRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new AuthenticationException("Utilisateur non trouvé"));

            // Révoquer les anciens refresh tokens
            refreshTokenRepository.revokeAllByUtilisateur(utilisateur);

            // Générer les nouveaux tokens
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = createRefreshToken(utilisateur);

            log.info("Connexion réussie pour l'utilisateur: {}", request.getUsername());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getExpirationTime() / 1000)
                    .username(utilisateur.getUsername())
                    .email(utilisateur.getEmail())
                    .roles(utilisateur.getRoles().stream()
                            .map(Role::getNom)
                            .collect(Collectors.toList()))
                    .message("Connexion réussie")
                    .build();

        } catch (Exception e) {
            log.error("Échec de connexion pour l'utilisateur: {}", request.getUsername());
            throw new AuthenticationException("Nom d'utilisateur ou mot de passe incorrect");
        }
    }

    /**
     * Enregistre un nouvel utilisateur
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Vérifier si le nom d'utilisateur existe déjà
        if (utilisateurRepository.existsByUsername(request.getUsername())) {
            throw new AuthenticationException("Ce nom d'utilisateur est déjà utilisé");
        }

        // Vérifier si l'email existe déjà
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException("Cet email est déjà utilisé");
        }

        // Vérifier que les mots de passe correspondent
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AuthenticationException("Les mots de passe ne correspondent pas");
        }

        // Déterminer le rôle selon le type d'utilisateur
        String userType = request.getUserType().toUpperCase();
        Role role = roleRepository.findByNom(userType)
                .orElseGet(() -> {
                    Role newRole = new Role(userType);
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        // Créer l'utilisateur
        Utilisateur utilisateur = Utilisateur.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .actif(true)
                .roles(roles)
                .build();

        utilisateurRepository.save(utilisateur);

        // Créer l'entité Etudiant ou Formateur selon le type
        if ("ETUDIANT".equals(userType)) {
            // Générer un matricule unique
            String matricule = generateMatricule();
            
            Etudiant etudiant = Etudiant.builder()
                    .matricule(matricule)
                    .nom(request.getNom())
                    .prenom(request.getPrenom())
                    .email(request.getEmail())
                    .dateInscription(LocalDate.now())
                    .utilisateur(utilisateur)
                    .build();
            
            etudiantRepository.save(etudiant);
            log.info("Nouvel étudiant enregistré: {} {} (matricule: {})", request.getPrenom(), request.getNom(), matricule);
            
        } else if ("FORMATEUR".equals(userType)) {
            Formateur formateur = Formateur.builder()
                    .nom(request.getNom())
                    .prenom(request.getPrenom())
                    .email(request.getEmail())
                    .telephone(request.getTelephone())
                    .utilisateur(utilisateur)
                    .build();
            
            formateurRepository.save(formateur);
            log.info("Nouveau formateur enregistré: {} {}", request.getPrenom(), request.getNom());
        }

        log.info("Nouvel utilisateur enregistré: {} (type: {})", request.getUsername(), userType);

        // Connecter automatiquement l'utilisateur après l'inscription
        LoginRequest loginRequest = LoginRequest.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build();

        AuthResponse response = login(loginRequest);
        response.setMessage("Inscription réussie");
        return response;
    }

    /**
     * Génère un matricule unique pour un étudiant
     */
    private String generateMatricule() {
        String year = String.valueOf(LocalDate.now().getYear());
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ETU-" + year + "-" + uuid;
    }

    /**
     * Rafraîchit le token d'accès avec un refresh token
     * L'ancien access token et le refresh token utilisé seront invalidés (Rotation)
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String oldAccessToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new TokenRefreshException("Refresh token non trouvé"));

        if (!refreshToken.isValid()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Le refresh token est expiré ou révoqué. Veuillez vous reconnecter.");
        }

        // Révoquer l'ancien access token si fourni
        if (oldAccessToken != null && !oldAccessToken.isEmpty()) {
            revokeAccessToken(oldAccessToken, "refresh");
        }

        Utilisateur utilisateur = refreshToken.getUtilisateur();

        // ⚠️ ROTATION : Révoquer l'ancien refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Créer un UserDetails pour générer les nouveaux tokens
        org.springframework.security.core.userdetails.User userDetails = 
            new org.springframework.security.core.userdetails.User(
                utilisateur.getUsername(),
                utilisateur.getPassword(),
                utilisateur.getRoles().stream()
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.getNom()))
                    .collect(Collectors.toList())
            );

        // Générer un NOUVEAU access token
        String newAccessToken = jwtService.generateToken(userDetails);
        
        // Générer un NOUVEAU refresh token (rotation)
        String newRefreshToken = createRefreshToken(utilisateur);

        log.info("Token rafraîchi pour l'utilisateur: {} (refresh token rotation)", utilisateur.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)  // ← Nouveau refresh token !
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime() / 1000)
                .username(utilisateur.getUsername())
                .email(utilisateur.getEmail())
                .roles(utilisateur.getRoles().stream()
                        .map(Role::getNom)
                        .collect(Collectors.toList()))
                .message("Token rafraîchi avec succès")
                .build();
    }

    /**
     * Déconnecte l'utilisateur en révoquant ses tokens
     */
    @Transactional
    public void logout(String username, String accessToken) {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Utilisateur non trouvé"));

        // Révoquer tous les refresh tokens de l'utilisateur
        refreshTokenRepository.revokeAllByUtilisateur(utilisateur);
        
        // Ajouter l'access token à la blacklist
        if (accessToken != null && !accessToken.isEmpty()) {
            revokeAccessToken(accessToken, "logout");
        }
        
        log.info("Déconnexion réussie pour l'utilisateur: {}", username);
    }

    /**
     * Révoque un access token (l'ajoute à la blacklist)
     */
    @Transactional
    public void revokeAccessToken(String token, String reason) {
        try {
            // Récupérer la date d'expiration du token
            Instant expiresAt = jwtService.extractExpiration(token).toInstant();
            
            RevokedToken revokedToken = RevokedToken.builder()
                    .token(token)
                    .revokedAt(Instant.now())
                    .expiresAt(expiresAt)
                    .reason(reason)
                    .build();
            
            revokedTokenRepository.save(revokedToken);
            log.debug("Token révoqué: raison={}", reason);
        } catch (Exception e) {
            log.warn("Impossible de révoquer le token: {}", e.getMessage());
        }
    }

    /**
     * Crée un nouveau refresh token
     */
    private String createRefreshToken(Utilisateur utilisateur) {
        // Créer un UserDetails temporaire pour générer le refresh token
        org.springframework.security.core.userdetails.User userDetails = 
            new org.springframework.security.core.userdetails.User(
                utilisateur.getUsername(),
                utilisateur.getPassword(),
                utilisateur.getRoles().stream()
                    .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.getNom()))
                    .collect(Collectors.toList())
            );

        String token = jwtService.generateRefreshToken(userDetails);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .utilisateur(utilisateur)
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationTime()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
