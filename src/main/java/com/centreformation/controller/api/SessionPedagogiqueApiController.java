package com.centreformation.controller.api;

import com.centreformation.entity.SessionPedagogique;
import com.centreformation.service.SessionPedagogiqueService;
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
 * API REST pour la gestion des sessions pédagogiques
 * 
 * Endpoints:
 * - GET    /api/sessions          - Liste toutes les sessions (paginé)
 * - GET    /api/sessions/{id}     - Détails d'une session
 * - POST   /api/sessions          - Créer une session (ADMIN)
 * - PUT    /api/sessions/{id}     - Modifier une session (ADMIN)
 * - DELETE /api/sessions/{id}     - Supprimer une session (ADMIN)
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionPedagogiqueApiController {

    private final SessionPedagogiqueService sessionService;

    /**
     * GET /api/sessions
     * Liste toutes les sessions pédagogiques avec pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<SessionPedagogique>> getAllSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "anneeScolaire") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SessionPedagogique> sessions = sessionService.findAll(pageable);
        return ResponseEntity.ok(sessions);
    }

    /**
     * GET /api/sessions/all
     * Liste toutes les sessions sans pagination (pour les selects)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<SessionPedagogique>> getAllSessionsList() {
        List<SessionPedagogique> sessions = sessionService.findAll();
        return ResponseEntity.ok(sessions);
    }

    /**
     * GET /api/sessions/{id}
     * Récupère une session par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<SessionPedagogique> getSessionById(@PathVariable Long id) {
        SessionPedagogique session = sessionService.findById(id);
        return ResponseEntity.ok(session);
    }

    /**
     * POST /api/sessions
     * Crée une nouvelle session pédagogique (ADMIN uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SessionPedagogique> createSession(@Valid @RequestBody SessionPedagogique session) {
        SessionPedagogique created = sessionService.save(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/sessions/{id}
     * Modifie une session existante (ADMIN uniquement)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SessionPedagogique> updateSession(
            @PathVariable Long id, 
            @Valid @RequestBody SessionPedagogique session) {
        SessionPedagogique updated = sessionService.update(id, session);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/sessions/{id}
     * Supprime une session (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable Long id) {
        sessionService.deleteById(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Session pédagogique supprimée avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/sessions/search
     * Recherche des sessions par année scolaire
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<SessionPedagogique>> searchSessions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SessionPedagogique> sessions = sessionService.search(keyword, pageable);
        return ResponseEntity.ok(sessions);
    }

    /**
     * GET /api/sessions/current
     * Récupère la session pédagogique en cours
     */
    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<SessionPedagogique> getCurrentSession() {
        SessionPedagogique session = sessionService.findCurrentSession();
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * GET /api/sessions/count
     * Compte le nombre total de sessions
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> countSessions() {
        Map<String, Long> response = new HashMap<>();
        response.put("count", sessionService.count());
        return ResponseEntity.ok(response);
    }
}
