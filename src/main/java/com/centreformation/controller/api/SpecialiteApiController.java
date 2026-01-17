package com.centreformation.controller.api;

import com.centreformation.entity.Specialite;
import com.centreformation.service.SpecialiteService;
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
 * API REST pour la gestion des spécialités
 * 
 * Endpoints:
 * - GET    /api/specialites          - Liste toutes les spécialités (paginé)
 * - GET    /api/specialites/{id}     - Détails d'une spécialité
 * - POST   /api/specialites          - Créer une spécialité (ADMIN)
 * - PUT    /api/specialites/{id}     - Modifier une spécialité (ADMIN)
 * - DELETE /api/specialites/{id}     - Supprimer une spécialité (ADMIN)
 */
@RestController
@RequestMapping("/api/specialites")
@RequiredArgsConstructor
public class SpecialiteApiController {

    private final SpecialiteService specialiteService;

    /**
     * GET /api/specialites
     * Liste toutes les spécialités avec pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<Page<Specialite>> getAllSpecialites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nom") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Specialite> specialites = specialiteService.findAll(pageable);
        return ResponseEntity.ok(specialites);
    }

    /**
     * GET /api/specialites/all
     * Liste toutes les spécialités sans pagination (pour les selects)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<List<Specialite>> getAllSpecialitesList() {
        List<Specialite> specialites = specialiteService.findAll();
        return ResponseEntity.ok(specialites);
    }

    /**
     * GET /api/specialites/{id}
     * Récupère une spécialité par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<Specialite> getSpecialiteById(@PathVariable Long id) {
        Specialite specialite = specialiteService.findById(id);
        return ResponseEntity.ok(specialite);
    }

    /**
     * POST /api/specialites
     * Crée une nouvelle spécialité (ADMIN uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Specialite> createSpecialite(@Valid @RequestBody Specialite specialite) {
        Specialite created = specialiteService.save(specialite);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/specialites/{id}
     * Modifie une spécialité existante (ADMIN uniquement)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Specialite> updateSpecialite(
            @PathVariable Long id, 
            @Valid @RequestBody Specialite specialite) {
        Specialite updated = specialiteService.update(id, specialite);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/specialites/{id}
     * Supprime une spécialité (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteSpecialite(@PathVariable Long id) {
        specialiteService.deleteById(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Spécialité supprimée avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/specialites/search
     * Recherche des spécialités par mot-clé
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Specialite>> searchSpecialites(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Specialite> specialites = specialiteService.search(keyword, pageable);
        return ResponseEntity.ok(specialites);
    }

    /**
     * GET /api/specialites/count
     * Compte le nombre total de spécialités
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Long>> countSpecialites() {
        Map<String, Long> response = new HashMap<>();
        response.put("count", specialiteService.count());
        return ResponseEntity.ok(response);
    }
}
