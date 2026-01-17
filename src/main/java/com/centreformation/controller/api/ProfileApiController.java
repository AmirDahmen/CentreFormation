package com.centreformation.controller.api;

import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Utilisateur;
import com.centreformation.repository.EtudiantRepository;
import com.centreformation.repository.FormateurRepository;
import com.centreformation.repository.UtilisateurRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API REST pour la gestion du profil utilisateur
 * 
 * Endpoints:
 * - GET    /api/profile           - Récupérer le profil de l'utilisateur connecté
 * - PUT    /api/profile           - Mettre à jour les informations du profil
 * - PUT    /api/profile/password  - Changer le mot de passe
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileApiController {

    private final UtilisateurRepository utilisateurRepository;
    private final EtudiantRepository etudiantRepository;
    private final FormateurRepository formateurRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * GET /api/profile
     * Récupère le profil de l'utilisateur connecté
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getProfile() {
        Utilisateur utilisateur = getCurrentUser();
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", utilisateur.getId());
        profile.put("username", utilisateur.getUsername());
        profile.put("email", utilisateur.getEmail());
        profile.put("roles", utilisateur.getRoles().stream()
                .map(role -> role.getNom())
                .toList());
        
        // Déterminer le type basé sur les rôles
        String userType = "USER";
        boolean hasRoleAdmin = utilisateur.getRoles().stream()
                .anyMatch(r -> r.getNom().equals("ROLE_ADMIN") || r.getNom().equals("ADMIN"));
        boolean hasRoleFormateur = utilisateur.getRoles().stream()
                .anyMatch(r -> r.getNom().equals("ROLE_FORMATEUR") || r.getNom().equals("FORMATEUR"));
        boolean hasRoleEtudiant = utilisateur.getRoles().stream()
                .anyMatch(r -> r.getNom().equals("ROLE_ETUDIANT") || r.getNom().equals("ETUDIANT"));
        
        if (hasRoleAdmin) {
            userType = "ADMIN";
        } else if (hasRoleFormateur) {
            userType = "FORMATEUR";
        } else if (hasRoleEtudiant) {
            userType = "ETUDIANT";
        }
        
        // Ajouter les informations spécifiques selon le rôle
        if (utilisateur.getEtudiantLie() != null) {
            Etudiant etudiant = utilisateur.getEtudiantLie();
            profile.put("type", "ETUDIANT");
            profile.put("nom", etudiant.getNom());
            profile.put("prenom", etudiant.getPrenom());
            profile.put("matricule", etudiant.getMatricule());
            profile.put("telephone", etudiant.getEmail());
            profile.put("dateInscription", etudiant.getDateInscription());
            if (etudiant.getSpecialite() != null) {
                profile.put("specialite", etudiant.getSpecialite().getNom());
            }
        } else if (utilisateur.getFormateurLie() != null) {
            Formateur formateur = utilisateur.getFormateurLie();
            profile.put("type", "FORMATEUR");
            profile.put("nom", formateur.getNom());
            profile.put("prenom", formateur.getPrenom());
            profile.put("telephone", formateur.getTelephone());
            if (formateur.getSpecialite() != null) {
                profile.put("specialite", formateur.getSpecialite().getNom());
            }
        } else {
            // Utiliser le type basé sur le rôle
            profile.put("type", userType);
            profile.put("nom", utilisateur.getUsername());
            profile.put("prenom", "");
        }
        
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/profile
     * Met à jour les informations du profil
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Utilisateur utilisateur = getCurrentUser();
        
        // Vérifier si l'email est déjà utilisé par un autre utilisateur
        if (request.getEmail() != null && !request.getEmail().equals(utilisateur.getEmail())) {
            if (utilisateurRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cet email est déjà utilisé"
                ));
            }
            utilisateur.setEmail(request.getEmail());
        }
        
        utilisateurRepository.save(utilisateur);
        
        String nom = request.getNom();
        String prenom = request.getPrenom();
        
        // Mettre à jour également l'entité liée (Etudiant ou Formateur)
        if (utilisateur.getEtudiantLie() != null) {
            Etudiant etudiant = utilisateur.getEtudiantLie();
            if (request.getNom() != null) etudiant.setNom(request.getNom());
            if (request.getPrenom() != null) etudiant.setPrenom(request.getPrenom());
            if (request.getEmail() != null) etudiant.setEmail(request.getEmail());
            etudiantRepository.save(etudiant);
            nom = etudiant.getNom();
            prenom = etudiant.getPrenom();
        } else if (utilisateur.getFormateurLie() != null) {
            Formateur formateur = utilisateur.getFormateurLie();
            if (request.getNom() != null) formateur.setNom(request.getNom());
            if (request.getPrenom() != null) formateur.setPrenom(request.getPrenom());
            if (request.getEmail() != null) formateur.setEmail(request.getEmail());
            if (request.getTelephone() != null) formateur.setTelephone(request.getTelephone());
            formateurRepository.save(formateur);
            nom = formateur.getNom();
            prenom = formateur.getPrenom();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profil mis à jour avec succès");
        response.put("nom", nom);
        response.put("prenom", prenom);
        response.put("email", utilisateur.getEmail());
        
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/profile/password
     * Change le mot de passe de l'utilisateur
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Utilisateur utilisateur = getCurrentUser();
        
        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(request.getOldPassword(), utilisateur.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "L'ancien mot de passe est incorrect"
            ));
        }
        
        // Vérifier que le nouveau mot de passe est différent de l'ancien
        if (request.getOldPassword().equals(request.getNewPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Le nouveau mot de passe doit être différent de l'ancien"
            ));
        }
        
        // Vérifier la confirmation du mot de passe
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La confirmation du mot de passe ne correspond pas"
            ));
        }
        
        // Mettre à jour le mot de passe
        utilisateur.setPassword(passwordEncoder.encode(request.getNewPassword()));
        utilisateurRepository.save(utilisateur);
        
        return ResponseEntity.ok(Map.of(
                "message", "Mot de passe modifié avec succès"
        ));
    }

    /**
     * Récupère l'utilisateur actuellement connecté
     */
    private Utilisateur getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    // DTO pour la mise à jour du profil
    @Data
    public static class UpdateProfileRequest {
        private String nom;
        private String prenom;
        private String email;
        private String telephone;
    }

    // DTO pour le changement de mot de passe
    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "L'ancien mot de passe est obligatoire")
        private String oldPassword;

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
        private String newPassword;

        @NotBlank(message = "La confirmation du mot de passe est obligatoire")
        private String confirmPassword;
    }
}
