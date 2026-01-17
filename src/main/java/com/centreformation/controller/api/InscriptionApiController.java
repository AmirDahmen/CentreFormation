package com.centreformation.controller.api;

import com.centreformation.entity.Inscription;
import com.centreformation.service.InscriptionService;
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
 * API REST pour la gestion des inscriptions
 * 
 * Endpoints:
 * - GET    /api/inscriptions           - Liste toutes les inscriptions (paginé)
 * - GET    /api/inscriptions/{id}      - Détails d'une inscription
 * - POST   /api/inscriptions           - Créer une inscription
 * - POST   /api/inscriptions/inscrire  - Inscrire un étudiant à un cours
 * - PUT    /api/inscriptions/{id}      - Modifier une inscription
 * - PUT    /api/inscriptions/{id}/annuler - Annuler une inscription
 * - DELETE /api/inscriptions/{id}      - Supprimer une inscription
 */
@RestController
@RequestMapping("/api/inscriptions")
@RequiredArgsConstructor
public class InscriptionApiController {

    private final InscriptionService inscriptionService;

    /**
     * GET /api/inscriptions
     * Liste toutes les inscriptions avec pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Inscription>> getAllInscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateInscription") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Inscription> inscriptions = inscriptionService.findAll(pageable);
        return ResponseEntity.ok(inscriptions);
    }

    /**
     * GET /api/inscriptions/all
     * Liste toutes les inscriptions sans pagination
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Inscription>> getAllInscriptionsList() {
        List<Inscription> inscriptions = inscriptionService.findAll();
        return ResponseEntity.ok(inscriptions);
    }

    /**
     * GET /api/inscriptions/{id}
     * Récupère une inscription par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isOwnerOfInscription(#id)")
    public ResponseEntity<Inscription> getInscriptionById(@PathVariable Long id) {
        Inscription inscription = inscriptionService.findById(id);
        return ResponseEntity.ok(inscription);
    }

    /**
     * GET /api/inscriptions/search
     * Recherche des inscriptions avec filtres
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Inscription>> searchInscriptions(
            @RequestParam(required = false) Long etudiantId,
            @RequestParam(required = false) Long coursId,
            @RequestParam(required = false) Inscription.StatutInscription statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Inscription> inscriptions = inscriptionService.search(etudiantId, coursId, statut, pageable);
        return ResponseEntity.ok(inscriptions);
    }

    /**
     * GET /api/inscriptions/statut/{statut}
     * Liste les inscriptions par statut
     */
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Inscription>> getInscriptionsByStatut(
            @PathVariable Inscription.StatutInscription statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Inscription> inscriptions = inscriptionService.findByStatut(statut, pageable);
        return ResponseEntity.ok(inscriptions);
    }

    /**
     * POST /api/inscriptions
     * Crée une nouvelle inscription (ADMIN uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Inscription> createInscription(@Valid @RequestBody Inscription inscription) {
        Inscription created = inscriptionService.save(inscription);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/inscriptions/inscrire
     * Inscrire un étudiant à un cours
     * Accessible aux étudiants pour s'inscrire eux-mêmes
     */
    @PostMapping("/inscrire")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#request.etudiantId)")
    public ResponseEntity<Inscription> inscrireEtudiant(@RequestBody InscriptionRequest request) {
        Inscription inscription = inscriptionService.inscrire(request.getEtudiantId(), request.getCoursId());
        return ResponseEntity.status(HttpStatus.CREATED).body(inscription);
    }

    /**
     * PUT /api/inscriptions/{id}
     * Modifie une inscription (ADMIN uniquement)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Inscription> updateInscription(
            @PathVariable Long id, 
            @Valid @RequestBody Inscription inscription) {
        Inscription updated = inscriptionService.update(id, inscription);
        return ResponseEntity.ok(updated);
    }

    /**
     * PUT /api/inscriptions/{id}/annuler
     * Annule une inscription
     */
    @PutMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isOwnerOfInscription(#id)")
    public ResponseEntity<Map<String, String>> annulerInscription(@PathVariable Long id) {
        inscriptionService.annuler(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Inscription annulée avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/inscriptions/{id}
     * Supprime une inscription (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteInscription(@PathVariable Long id) {
        inscriptionService.deleteById(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Inscription supprimée avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/inscriptions/etudiant/{etudiantId}
     * Liste les inscriptions d'un étudiant
     */
    @GetMapping("/etudiant/{etudiantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#etudiantId)")
    public ResponseEntity<List<Inscription>> getInscriptionsByEtudiant(@PathVariable Long etudiantId) {
        List<Inscription> inscriptions = inscriptionService.findByEtudiant(etudiantId);
        return ResponseEntity.ok(inscriptions);
    }

    /**
     * GET /api/inscriptions/cours/{coursId}
     * Liste les inscriptions d'un cours
     */
    @GetMapping("/cours/{coursId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Inscription>> getInscriptionsByCours(@PathVariable Long coursId) {
        List<Inscription> inscriptions = inscriptionService.findByCours(coursId);
        return ResponseEntity.ok(inscriptions);
    }

    /**
     * GET /api/inscriptions/count
     * Compte le nombre total d'inscriptions
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> countInscriptions() {
        Map<String, Object> response = new HashMap<>();
        response.put("total", inscriptionService.count());
        response.put("actives", inscriptionService.countByStatut(Inscription.StatutInscription.ACTIVE));
        response.put("annulees", inscriptionService.countByStatut(Inscription.StatutInscription.ANNULEE));
        response.put("terminees", inscriptionService.countByStatut(Inscription.StatutInscription.TERMINEE));
        return ResponseEntity.ok(response);
    }

    /**
     * DTO pour la requête d'inscription
     */
    @lombok.Data
    public static class InscriptionRequest {
        private Long etudiantId;
        private Long coursId;
    }
}
