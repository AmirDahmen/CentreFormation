package com.centreformation.controller.api;

import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Inscription;
import com.centreformation.entity.Note;
import com.centreformation.service.EtudiantService;
import com.centreformation.service.InscriptionService;
import com.centreformation.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST pour la gestion des étudiants
 * 
 * Endpoints:
 * - GET    /api/etudiants          - Liste tous les étudiants (paginé)
 * - GET    /api/etudiants/{id}     - Détails d'un étudiant
 * - POST   /api/etudiants          - Créer un étudiant (ADMIN)
 * - PUT    /api/etudiants/{id}     - Modifier un étudiant (ADMIN)
 * - DELETE /api/etudiants/{id}     - Supprimer un étudiant (ADMIN)
 * - GET    /api/etudiants/{id}/inscriptions - Inscriptions d'un étudiant
 * - GET    /api/etudiants/{id}/notes        - Notes d'un étudiant
 * - GET    /api/etudiants/{id}/moyenne      - Moyenne d'un étudiant
 * - GET    /api/etudiants/search            - Rechercher des étudiants
 */
@RestController
@RequestMapping("/api/etudiants")
@RequiredArgsConstructor
public class EtudiantApiController {

    private final EtudiantService etudiantService;
    private final InscriptionService inscriptionService;
    private final NoteService noteService;

    /**
     * GET /api/etudiants
     * Liste tous les étudiants avec pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Etudiant>> getAllEtudiants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Etudiant> etudiants = etudiantService.findAll(pageable);
        return ResponseEntity.ok(etudiants);
    }

    /**
     * GET /api/etudiants/{id}
     * Récupère un étudiant par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#id)")
    public ResponseEntity<Etudiant> getEtudiantById(@PathVariable Long id) {
        Etudiant etudiant = etudiantService.findById(id);
        return ResponseEntity.ok(etudiant);
    }

    /**
     * GET /api/etudiants/matricule/{matricule}
     * Récupère un étudiant par son matricule
     */
    @GetMapping("/matricule/{matricule}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Etudiant> getEtudiantByMatricule(@PathVariable String matricule) {
        Etudiant etudiant = etudiantService.findByMatricule(matricule);
        return ResponseEntity.ok(etudiant);
    }

    /**
     * POST /api/etudiants
     * Crée un nouvel étudiant (ADMIN uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Etudiant> createEtudiant(@Valid @RequestBody Etudiant etudiant) {
        Etudiant created = etudiantService.save(etudiant);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/etudiants/{id}
     * Modifie un étudiant existant (ADMIN uniquement)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Etudiant> updateEtudiant(
            @PathVariable Long id, 
            @Valid @RequestBody Etudiant etudiant) {
        Etudiant updated = etudiantService.update(id, etudiant);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/etudiants/{id}
     * Supprime un étudiant (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteEtudiant(@PathVariable Long id) {
        etudiantService.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Étudiant supprimé avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/etudiants/search
     * Recherche des étudiants par mot-clé
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Etudiant>> searchEtudiants(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Etudiant> etudiants = etudiantService.search(keyword, pageable);
        return ResponseEntity.ok(etudiants);
    }

    /**
     * GET /api/etudiants/specialite/{specialiteId}
     * Liste les étudiants par spécialité
     */
    @GetMapping("/specialite/{specialiteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Etudiant>> getEtudiantsBySpecialite(
            @PathVariable Long specialiteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Etudiant> etudiants = etudiantService.findBySpecialite(specialiteId, pageable);
        return ResponseEntity.ok(etudiants);
    }

    /**
     * GET /api/etudiants/{id}/inscriptions
     * Récupère les inscriptions d'un étudiant
     */
    @GetMapping("/{id}/inscriptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#id)")
    public ResponseEntity<List<Inscription>> getInscriptionsByEtudiant(@PathVariable Long id) {
        List<Inscription> inscriptions = inscriptionService.findByEtudiant(id);
        return ResponseEntity.ok(inscriptions);
    }

    /**
     * GET /api/etudiants/{id}/notes
     * Récupère les notes d'un étudiant
     */
    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#id)")
    public ResponseEntity<List<Note>> getNotesByEtudiant(@PathVariable Long id) {
        List<Note> notes = noteService.findByEtudiant(id);
        return ResponseEntity.ok(notes);
    }

    /**
     * GET /api/etudiants/{id}/moyenne
     * Calcule la moyenne générale d'un étudiant
     */
    @GetMapping("/{id}/moyenne")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#id)")
    public ResponseEntity<Map<String, Object>> getMoyenneByEtudiant(@PathVariable Long id) {
        Etudiant etudiant = etudiantService.findById(id);
        Double moyenne = noteService.calculateAverageByEtudiant(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("etudiantId", id);
        response.put("etudiantNom", etudiant.getNom() + " " + etudiant.getPrenom());
        response.put("moyenne", moyenne != null ? moyenne : "Aucune note");
        response.put("reussite", moyenne != null && moyenne >= 10);
        
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/etudiants/{etudiantId}/groupes/{groupeId}
     * Ajoute un étudiant à un groupe
     */
    @PutMapping("/{etudiantId}/groupes/{groupeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> addGroupeToEtudiant(
            @PathVariable Long etudiantId,
            @PathVariable Long groupeId) {
        etudiantService.addGroupeToEtudiant(etudiantId, groupeId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Étudiant ajouté au groupe avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/etudiants/{etudiantId}/groupes/{groupeId}
     * Retire un étudiant d'un groupe
     */
    @DeleteMapping("/{etudiantId}/groupes/{groupeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> removeGroupeFromEtudiant(
            @PathVariable Long etudiantId,
            @PathVariable Long groupeId) {
        etudiantService.removeGroupeFromEtudiant(etudiantId, groupeId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Étudiant retiré du groupe avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/etudiants/count
     * Compte le nombre total d'étudiants
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Long>> countEtudiants() {
        Map<String, Long> response = new HashMap<>();
        response.put("count", etudiantService.count());
        return ResponseEntity.ok(response);
    }
}
