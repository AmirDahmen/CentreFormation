package com.centreformation.controller.api;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Formateur;
import com.centreformation.service.FormateurService;
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
 * API REST pour la gestion des formateurs
 * 
 * Endpoints:
 * - GET    /api/formateurs          - Liste tous les formateurs (paginé)
 * - GET    /api/formateurs/{id}     - Détails d'un formateur
 * - POST   /api/formateurs          - Créer un formateur (ADMIN)
 * - PUT    /api/formateurs/{id}     - Modifier un formateur (ADMIN)
 * - DELETE /api/formateurs/{id}     - Supprimer un formateur (ADMIN)
 * - GET    /api/formateurs/{id}/cours - Cours d'un formateur
 * - GET    /api/formateurs/search   - Rechercher des formateurs
 */
@RestController
@RequestMapping("/api/formateurs")
@RequiredArgsConstructor
public class FormateurApiController {

    private final FormateurService formateurService;

    /**
     * GET /api/formateurs
     * Liste tous les formateurs avec pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Formateur>> getAllFormateurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Formateur> formateurs = formateurService.findAll(pageable);
        return ResponseEntity.ok(formateurs);
    }

    /**
     * GET /api/formateurs/all
     * Liste tous les formateurs sans pagination (pour les selects)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Formateur>> getAllFormateursList() {
        List<Formateur> formateurs = formateurService.findAll();
        return ResponseEntity.ok(formateurs);
    }

    /**
     * GET /api/formateurs/{id}
     * Récupère un formateur par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Formateur> getFormateurById(@PathVariable Long id) {
        Formateur formateur = formateurService.findById(id);
        return ResponseEntity.ok(formateur);
    }

    /**
     * POST /api/formateurs
     * Crée un nouveau formateur (ADMIN uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Formateur> createFormateur(@Valid @RequestBody Formateur formateur) {
        Formateur created = formateurService.save(formateur);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/formateurs/{id}
     * Modifie un formateur existant (ADMIN uniquement)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Formateur> updateFormateur(
            @PathVariable Long id, 
            @Valid @RequestBody Formateur formateur) {
        Formateur updated = formateurService.update(id, formateur);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/formateurs/{id}
     * Supprime un formateur (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteFormateur(@PathVariable Long id) {
        formateurService.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Formateur supprimé avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/formateurs/search
     * Recherche des formateurs par mot-clé
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Formateur>> searchFormateurs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Formateur> formateurs = formateurService.search(keyword, pageable);
        return ResponseEntity.ok(formateurs);
    }

    /**
     * GET /api/formateurs/specialite/{specialiteId}
     * Liste les formateurs par spécialité
     */
    @GetMapping("/specialite/{specialiteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Formateur>> getFormateursBySpecialite(
            @PathVariable Long specialiteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Formateur> formateurs = formateurService.findBySpecialite(specialiteId, pageable);
        return ResponseEntity.ok(formateurs);
    }

    /**
     * GET /api/formateurs/{id}/cours
     * Récupère les cours dispensés par un formateur
     */
    @GetMapping("/{id}/cours")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Cours>> getCoursByFormateur(@PathVariable Long id) {
        List<Cours> cours = formateurService.findCoursByFormateur(id);
        return ResponseEntity.ok(cours);
    }

    /**
     * GET /api/formateurs/count
     * Compte le nombre total de formateurs
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> countFormateurs() {
        Map<String, Long> response = new HashMap<>();
        response.put("count", formateurService.count());
        return ResponseEntity.ok(response);
    }
}
