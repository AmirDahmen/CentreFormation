package com.centreformation.controller.api;

import com.centreformation.entity.Groupe;
import com.centreformation.service.GroupeService;
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
 * API REST pour la gestion des groupes
 * 
 * Endpoints:
 * - GET    /api/groupes          - Liste tous les groupes (paginé)
 * - GET    /api/groupes/{id}     - Détails d'un groupe
 * - POST   /api/groupes          - Créer un groupe (ADMIN)
 * - PUT    /api/groupes/{id}     - Modifier un groupe (ADMIN)
 * - DELETE /api/groupes/{id}     - Supprimer un groupe (ADMIN)
 */
@RestController
@RequestMapping("/api/groupes")
@RequiredArgsConstructor
public class GroupeApiController {

    private final GroupeService groupeService;

    /**
     * GET /api/groupes
     * Liste tous les groupes avec pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Groupe>> getAllGroupes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Groupe> groupes = groupeService.findAll(pageable);
        return ResponseEntity.ok(groupes);
    }

    /**
     * GET /api/groupes/all
     * Liste tous les groupes sans pagination (pour les selects)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Groupe>> getAllGroupesList() {
        List<Groupe> groupes = groupeService.findAll();
        return ResponseEntity.ok(groupes);
    }

    /**
     * GET /api/groupes/{id}
     * Récupère un groupe par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<Groupe> getGroupeById(@PathVariable Long id) {
        Groupe groupe = groupeService.findById(id);
        return ResponseEntity.ok(groupe);
    }

    /**
     * POST /api/groupes
     * Crée un nouveau groupe (ADMIN uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Groupe> createGroupe(@Valid @RequestBody Groupe groupe) {
        Groupe created = groupeService.save(groupe);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/groupes/{id}
     * Modifie un groupe existant (ADMIN uniquement)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Groupe> updateGroupe(
            @PathVariable Long id, 
            @Valid @RequestBody Groupe groupe) {
        Groupe updated = groupeService.update(id, groupe);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/groupes/{id}
     * Supprime un groupe (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteGroupe(@PathVariable Long id) {
        groupeService.deleteById(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Groupe supprimé avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/groupes/search
     * Recherche des groupes par mot-clé
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Groupe>> searchGroupes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Groupe> groupes = groupeService.search(keyword, pageable);
        return ResponseEntity.ok(groupes);
    }

    /**
     * GET /api/groupes/specialite/{specialiteId}
     * Liste les groupes par spécialité
     */
    @GetMapping("/specialite/{specialiteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Groupe>> getGroupesBySpecialite(@PathVariable Long specialiteId) {
        List<Groupe> groupes = groupeService.findBySpecialite(specialiteId);
        return ResponseEntity.ok(groupes);
    }

    /**
     * GET /api/groupes/session/{sessionId}
     * Liste les groupes par session pédagogique
     */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Groupe>> getGroupesBySession(@PathVariable Long sessionId) {
        List<Groupe> groupes = groupeService.findBySessionPedagogique(sessionId);
        return ResponseEntity.ok(groupes);
    }

    /**
     * GET /api/groupes/count
     * Compte le nombre total de groupes
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Long>> countGroupes() {
        Map<String, Long> response = new HashMap<>();
        response.put("count", groupeService.count());
        return ResponseEntity.ok(response);
    }
}
