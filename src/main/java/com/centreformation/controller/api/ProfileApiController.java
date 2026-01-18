package com.centreformation.controller.api;

import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Utilisateur;
import com.centreformation.entity.Cours;
import com.centreformation.entity.Inscription;
import com.centreformation.entity.Groupe;
import com.centreformation.entity.SeanceCours;
import com.centreformation.entity.Note;
import com.centreformation.repository.EtudiantRepository;
import com.centreformation.repository.FormateurRepository;
import com.centreformation.repository.UtilisateurRepository;
import com.centreformation.repository.InscriptionRepository;
import com.centreformation.repository.SeanceCoursRepository;
import com.centreformation.repository.NoteRepository;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final InscriptionRepository inscriptionRepository;
    private final SeanceCoursRepository seanceCoursRepository;
    private final NoteRepository noteRepository;
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
        Etudiant etudiant = utilisateur.getEtudiantLie();
        // Si pas d'étudiant lié directement, chercher par email
        if (etudiant == null && hasRoleEtudiant) {
            etudiant = etudiantRepository.findByEmail(utilisateur.getEmail()).orElse(null);
            // Lier l'étudiant trouvé à l'utilisateur pour les prochaines fois
            if (etudiant != null && etudiant.getUtilisateur() == null) {
                etudiant.setUtilisateur(utilisateur);
                etudiantRepository.save(etudiant);
            }
        }
        
        if (etudiant != null) {
            profile.put("type", "ETUDIANT");
            profile.put("etudiantId", etudiant.getId());
            profile.put("nom", etudiant.getNom());
            profile.put("prenom", etudiant.getPrenom());
            profile.put("matricule", etudiant.getMatricule());
            profile.put("telephone", etudiant.getEmail());
            profile.put("dateInscription", etudiant.getDateInscription());
            if (etudiant.getSpecialite() != null) {
                profile.put("specialite", etudiant.getSpecialite().getNom());
            }
            
            // Ajouter les inscriptions directes avec leurs cours
            List<Inscription> inscriptions = inscriptionRepository.findByEtudiant(etudiant);
            List<Map<String, Object>> inscriptionsData = inscriptions.stream()
                .map(inscription -> {
                    Map<String, Object> inscriptionMap = new HashMap<>();
                    inscriptionMap.put("id", inscription.getId());
                    inscriptionMap.put("dateInscription", inscription.getDateInscription());
                    inscriptionMap.put("statut", inscription.getStatut() != null ? inscription.getStatut().name() : "EN_ATTENTE");
                    
                    // Ajouter le groupe si l'inscription est validée
                    if (inscription.getGroupe() != null) {
                        Map<String, Object> groupeMap = new HashMap<>();
                        groupeMap.put("id", inscription.getGroupe().getId());
                        groupeMap.put("nom", inscription.getGroupe().getNom());
                        groupeMap.put("code", inscription.getGroupe().getCode());
                        inscriptionMap.put("groupe", groupeMap);
                    }
                    
                    if (inscription.getCours() != null) {
                        Cours cours = inscription.getCours();
                        Map<String, Object> coursMap = new HashMap<>();
                        coursMap.put("id", cours.getId());
                        coursMap.put("code", cours.getCode());
                        coursMap.put("titre", cours.getTitre());
                        coursMap.put("description", cours.getDescription());
                        coursMap.put("duree", cours.getNombreHeures());
                        if (cours.getFormateur() != null) {
                            coursMap.put("formateur", cours.getFormateur().getPrenom() + " " + cours.getFormateur().getNom());
                        }
                        inscriptionMap.put("cours", coursMap);
                    }
                    return inscriptionMap;
                })
                .collect(Collectors.toList());
            profile.put("inscriptions", inscriptionsData);
            
            // Ajouter les notes de l'étudiant (toutes les notes qui ont une valeur)
            List<Note> notesEtudiant = noteRepository.findByEtudiant(etudiant);
            List<Map<String, Object>> notesData = notesEtudiant.stream()
                .filter(note -> note.getValeur() != null)
                .map(note -> {
                    Map<String, Object> noteMap = new HashMap<>();
                    noteMap.put("id", note.getId());
                    noteMap.put("valeur", note.getValeur());
                    noteMap.put("commentaire", note.getCommentaire());
                    noteMap.put("dateSaisie", note.getDateSaisie());
                    if (note.getCours() != null) {
                        noteMap.put("coursId", note.getCours().getId());
                        noteMap.put("coursCode", note.getCours().getCode());
                        noteMap.put("coursTitre", note.getCours().getTitre());
                        if (note.getCours().getFormateur() != null) {
                            noteMap.put("formateur", note.getCours().getFormateur().getPrenom() + " " + 
                                       note.getCours().getFormateur().getNom());
                        }
                    }
                    return noteMap;
                })
                .collect(Collectors.toList());
            profile.put("notes", notesData);
            
            // Calculer la moyenne
            if (!notesData.isEmpty()) {
                double somme = notesData.stream()
                    .mapToDouble(n -> ((Number) n.get("valeur")).doubleValue())
                    .sum();
                profile.put("moyenne", Math.round((somme / notesData.size()) * 100.0) / 100.0);
            }
            
            // Ajouter les cours via les groupes de l'étudiant
            List<Map<String, Object>> coursViaGroupes = new java.util.ArrayList<>();
            Set<Long> seenCoursIds = new HashSet<>();
            for (Groupe groupe : etudiant.getGroupes()) {
                // NOUVEAU: un groupe appartient à UN seul cours
                Cours cours = groupe.getCours();
                if (cours == null) continue;
                
                // Éviter les doublons si l'étudiant est dans plusieurs groupes du même cours
                if (seenCoursIds.contains(cours.getId())) continue;
                seenCoursIds.add(cours.getId());
                
                Map<String, Object> coursMap = new HashMap<>();
                coursMap.put("id", cours.getId());
                coursMap.put("code", cours.getCode());
                coursMap.put("titre", cours.getTitre());
                coursMap.put("description", cours.getDescription());
                coursMap.put("duree", cours.getNombreHeures());
                coursMap.put("groupe", groupe.getNom());
                if (cours.getFormateur() != null) {
                    coursMap.put("formateur", cours.getFormateur().getPrenom() + " " + cours.getFormateur().getNom());
                }
                coursViaGroupes.add(coursMap);
            }
            profile.put("coursViaGroupes", coursViaGroupes);
            
            // Ajouter l'emploi du temps de l'étudiant (séances des inscriptions VALIDÉES uniquement)
            List<Map<String, Object>> emploiDuTemps = new java.util.ArrayList<>();
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusMonths(2); // 2 mois à venir
            Set<Long> seenSeanceIds = new HashSet<>();
            
            // Parcourir les inscriptions VALIDÉES de l'étudiant
            for (Inscription inscription : inscriptions) {
                // Ne prendre que les inscriptions VALIDÉES avec un groupe assigné
                if (inscription.getStatut() != Inscription.StatutInscription.VALIDEE || inscription.getGroupe() == null) {
                    continue;
                }
                
                Groupe groupe = inscription.getGroupe();
                Cours coursInscription = inscription.getCours();
                List<SeanceCours> seances = seanceCoursRepository.findByGroupe(groupe);
                
                for (SeanceCours seance : seances) {
                    // Éviter les doublons
                    if (seenSeanceIds.contains(seance.getId())) continue;
                    
                    // Ne prendre que les séances du cours pour lequel l'étudiant est inscrit
                    if (coursInscription != null && seance.getCours() != null 
                        && !seance.getCours().getId().equals(coursInscription.getId())) {
                        continue;
                    }
                    
                    seenSeanceIds.add(seance.getId());
                    
                    // Inclure les séances passées (1 mois) et futures (2 mois)
                    if (seance.getDate().isAfter(today.minusMonths(1)) && seance.getDate().isBefore(endDate)) {
                        Map<String, Object> seanceMap = new HashMap<>();
                        seanceMap.put("id", seance.getId());
                        seanceMap.put("date", seance.getDate().toString());
                        seanceMap.put("heureDebut", seance.getHeureDebut().toString());
                        seanceMap.put("heureFin", seance.getHeureFin().toString());
                        seanceMap.put("contenu", seance.getContenu());
                        seanceMap.put("groupe", groupe.getNom());
                        
                        if (seance.getCours() != null) {
                            seanceMap.put("coursId", seance.getCours().getId());
                            seanceMap.put("coursTitre", seance.getCours().getTitre());
                            seanceMap.put("coursCode", seance.getCours().getCode());
                        }
                        if (seance.getFormateur() != null) {
                            seanceMap.put("formateur", seance.getFormateur().getPrenom() + " " + seance.getFormateur().getNom());
                        }
                        if (seance.getSalle() != null) {
                            seanceMap.put("salle", seance.getSalle().getNom());
                        } else if (seance.getSalleTexte() != null) {
                            seanceMap.put("salle", seance.getSalleTexte());
                        }
                        emploiDuTemps.add(seanceMap);
                    }
                }
            }
            // Trier par date et heure
            emploiDuTemps.sort((a, b) -> {
                int dateCompare = ((String) a.get("date")).compareTo((String) b.get("date"));
                if (dateCompare != 0) return dateCompare;
                return ((String) a.get("heureDebut")).compareTo((String) b.get("heureDebut"));
            });
            profile.put("emploiDuTemps", emploiDuTemps);
            
        } else if (hasRoleFormateur) {
            Formateur formateur = utilisateur.getFormateurLie();
            // Si pas de formateur lié directement, chercher par email
            if (formateur == null) {
                formateur = formateurRepository.findByEmail(utilisateur.getEmail()).orElse(null);
                // Lier le formateur trouvé à l'utilisateur pour les prochaines fois
                if (formateur != null && formateur.getUtilisateur() == null) {
                    formateur.setUtilisateur(utilisateur);
                    formateurRepository.save(formateur);
                }
            }
            
            if (formateur != null) {
                profile.put("type", "FORMATEUR");
                profile.put("formateurId", formateur.getId());
                profile.put("nom", formateur.getNom());
                profile.put("prenom", formateur.getPrenom());
                profile.put("telephone", formateur.getTelephone());
                if (formateur.getSpecialite() != null) {
                    profile.put("specialite", formateur.getSpecialite().getNom());
                }
                
                // Ajouter les cours dispensés par le formateur
                List<Map<String, Object>> coursDispenses = formateur.getCours().stream()
                    .map(cours -> {
                        Map<String, Object> coursMap = new HashMap<>();
                        coursMap.put("id", cours.getId());
                        coursMap.put("code", cours.getCode());
                        coursMap.put("titre", cours.getTitre());
                        coursMap.put("description", cours.getDescription());
                        coursMap.put("duree", cours.getNombreHeures());
                        // Ajouter les groupes associés à ce cours
                        List<String> groupesNoms = cours.getGroupes().stream()
                            .map(Groupe::getNom)
                            .collect(Collectors.toList());
                        coursMap.put("groupes", groupesNoms);
                        return coursMap;
                    })
                    .collect(Collectors.toList());
                profile.put("coursDispenses", coursDispenses);
                
                // Ajouter l'emploi du temps du formateur (ses séances)
                List<SeanceCours> seances = seanceCoursRepository.findByFormateur(formateur);
                LocalDate today = LocalDate.now();
                LocalDate endDate = today.plusMonths(2);
                
                List<Map<String, Object>> emploiDuTemps = seances.stream()
                    .filter(s -> s.getDate().isAfter(today.minusMonths(1)) && s.getDate().isBefore(endDate))
                    .sorted((a, b) -> {
                        int dateCompare = a.getDate().compareTo(b.getDate());
                        if (dateCompare != 0) return dateCompare;
                        return a.getHeureDebut().compareTo(b.getHeureDebut());
                    })
                    .map(seance -> {
                        Map<String, Object> seanceMap = new HashMap<>();
                        seanceMap.put("id", seance.getId());
                        seanceMap.put("date", seance.getDate().toString());
                        seanceMap.put("heureDebut", seance.getHeureDebut().toString());
                        seanceMap.put("heureFin", seance.getHeureFin().toString());
                        seanceMap.put("contenu", seance.getContenu());
                        
                        if (seance.getCours() != null) {
                            seanceMap.put("coursId", seance.getCours().getId());
                            seanceMap.put("coursTitre", seance.getCours().getTitre());
                            seanceMap.put("coursCode", seance.getCours().getCode());
                        }
                        if (seance.getGroupe() != null) {
                            seanceMap.put("groupe", seance.getGroupe().getNom());
                        }
                        if (seance.getSalle() != null) {
                            seanceMap.put("salle", seance.getSalle().getNom());
                        } else if (seance.getSalleTexte() != null) {
                            seanceMap.put("salle", seance.getSalleTexte());
                        }
                        return seanceMap;
                    })
                    .collect(Collectors.toList());
                profile.put("emploiDuTemps", emploiDuTemps);
            } else {
                profile.put("type", userType);
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
