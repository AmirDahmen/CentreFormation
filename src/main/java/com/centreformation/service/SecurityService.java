package com.centreformation.service;

import com.centreformation.entity.*;
import com.centreformation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service de sécurité pour les vérifications d'autorisation personnalisées
 * Utilisé avec @PreAuthorize dans les contrôleurs
 */
@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final UtilisateurRepository utilisateurRepository;
    private final EtudiantRepository etudiantRepository;
    private final FormateurRepository formateurRepository;
    private final InscriptionRepository inscriptionRepository;
    private final NoteRepository noteRepository;
    private final CoursRepository coursRepository;

    /**
     * Vérifie si l'utilisateur connecté est l'étudiant spécifié
     */
    public boolean isCurrentEtudiant(Long etudiantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        String username = auth.getName();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);
        
        if (utilisateur == null || utilisateur.getEtudiantLie() == null) {
            return false;
        }
        
        return utilisateur.getEtudiantLie().getId().equals(etudiantId);
    }

    /**
     * Vérifie si l'utilisateur connecté est le formateur spécifié
     */
    public boolean isCurrentFormateur(Long formateurId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        String username = auth.getName();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);
        
        if (utilisateur == null || utilisateur.getFormateurLie() == null) {
            return false;
        }
        
        return utilisateur.getFormateurLie().getId().equals(formateurId);
    }

    /**
     * Vérifie si l'utilisateur connecté est le formateur du cours spécifié
     */
    public boolean isFormateurOfCours(Long coursId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        String username = auth.getName();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);
        
        if (utilisateur == null || utilisateur.getFormateurLie() == null) {
            return false;
        }
        
        Cours cours = coursRepository.findById(coursId).orElse(null);
        if (cours == null || cours.getFormateur() == null) {
            return false;
        }
        
        return cours.getFormateur().getId().equals(utilisateur.getFormateurLie().getId());
    }

    /**
     * Vérifie si l'utilisateur connecté est le propriétaire de l'inscription
     */
    public boolean isOwnerOfInscription(Long inscriptionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        String username = auth.getName();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);
        
        if (utilisateur == null || utilisateur.getEtudiantLie() == null) {
            return false;
        }
        
        Inscription inscription = inscriptionRepository.findById(inscriptionId).orElse(null);
        if (inscription == null) {
            return false;
        }
        
        return inscription.getEtudiant().getId().equals(utilisateur.getEtudiantLie().getId());
    }

    /**
     * Vérifie si l'utilisateur connecté est le propriétaire de la note (étudiant)
     */
    public boolean isOwnerOfNote(Long noteId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        String username = auth.getName();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);
        
        if (utilisateur == null || utilisateur.getEtudiantLie() == null) {
            return false;
        }
        
        Note note = noteRepository.findById(noteId).orElse(null);
        if (note == null) {
            return false;
        }
        
        return note.getEtudiant().getId().equals(utilisateur.getEtudiantLie().getId());
    }

    /**
     * Récupère l'ID de l'étudiant lié à l'utilisateur connecté
     */
    public Long getCurrentEtudiantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        String username = auth.getName();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);
        
        if (utilisateur == null || utilisateur.getEtudiantLie() == null) {
            return null;
        }
        
        return utilisateur.getEtudiantLie().getId();
    }

    /**
     * Récupère l'ID du formateur lié à l'utilisateur connecté
     */
    public Long getCurrentFormateurId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        String username = auth.getName();
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);
        
        if (utilisateur == null || utilisateur.getFormateurLie() == null) {
            return null;
        }
        
        return utilisateur.getFormateurLie().getId();
    }

    /**
     * Vérifie si l'utilisateur connecté a un rôle spécifique
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
