package com.centreformation.controller.api;

import com.centreformation.entity.Note;
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
 * API REST pour la gestion des notes
 * 
 * Endpoints:
 * - GET    /api/notes           - Liste toutes les notes (paginé)
 * - GET    /api/notes/{id}      - Détails d'une note
 * - POST   /api/notes           - Créer une note (FORMATEUR/ADMIN)
 * - POST   /api/notes/saisir    - Saisir une note (FORMATEUR)
 * - PUT    /api/notes/{id}      - Modifier une note (FORMATEUR/ADMIN)
 * - DELETE /api/notes/{id}      - Supprimer une note (ADMIN)
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteApiController {

    private final NoteService noteService;

    /**
     * GET /api/notes
     * Liste toutes les notes avec pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Note>> getAllNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dateSaisie") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Note> notes = noteService.findAll(pageable);
        return ResponseEntity.ok(notes);
    }

    /**
     * GET /api/notes/all
     * Liste toutes les notes sans pagination
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Note>> getAllNotesList() {
        List<Note> notes = noteService.findAll();
        return ResponseEntity.ok(notes);
    }

    /**
     * GET /api/notes/{id}
     * Récupère une note par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isOwnerOfNote(#id)")
    public ResponseEntity<Note> getNoteById(@PathVariable Long id) {
        Note note = noteService.findById(id);
        return ResponseEntity.ok(note);
    }

    /**
     * POST /api/notes
     * Crée une nouvelle note
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Note> createNote(@Valid @RequestBody Note note) {
        Note created = noteService.save(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * POST /api/notes/saisir
     * Saisir une note pour un étudiant dans un cours
     */
    @PostMapping("/saisir")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Note> saisirNote(@RequestBody SaisieNoteRequest request) {
        Note note = noteService.saisirNote(
                request.getEtudiantId(),
                request.getCoursId(),
                request.getValeur(),
                request.getCommentaire(),
                request.getFormateurId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    /**
     * PUT /api/notes/{id}
     * Modifie une note existante
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Note> updateNote(
            @PathVariable Long id, 
            @Valid @RequestBody Note note) {
        Note updated = noteService.update(id, note);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/notes/{id}
     * Supprime une note (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteNote(@PathVariable Long id) {
        noteService.deleteById(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Note supprimée avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/notes/etudiant/{etudiantId}
     * Liste les notes d'un étudiant
     */
    @GetMapping("/etudiant/{etudiantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#etudiantId)")
    public ResponseEntity<List<Note>> getNotesByEtudiant(@PathVariable Long etudiantId) {
        List<Note> notes = noteService.findByEtudiant(etudiantId);
        return ResponseEntity.ok(notes);
    }

    /**
     * GET /api/notes/etudiant/{etudiantId}/paginated
     * Liste les notes d'un étudiant avec pagination
     */
    @GetMapping("/etudiant/{etudiantId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#etudiantId)")
    public ResponseEntity<Page<Note>> getNotesByEtudiantPaginated(
            @PathVariable Long etudiantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Note> notes = noteService.findByEtudiantId(etudiantId, pageable);
        return ResponseEntity.ok(notes);
    }

    /**
     * GET /api/notes/cours/{coursId}
     * Liste les notes d'un cours
     */
    @GetMapping("/cours/{coursId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Note>> getNotesByCours(@PathVariable Long coursId) {
        List<Note> notes = noteService.findByCours(coursId);
        return ResponseEntity.ok(notes);
    }

    /**
     * GET /api/notes/cours/{coursId}/paginated
     * Liste les notes d'un cours avec pagination
     */
    @GetMapping("/cours/{coursId}/paginated")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Note>> getNotesByCoursPaginated(
            @PathVariable Long coursId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Note> notes = noteService.findByCoursId(coursId, pageable);
        return ResponseEntity.ok(notes);
    }

    /**
     * GET /api/notes/moyenne/etudiant/{etudiantId}
     * Calcule la moyenne d'un étudiant
     */
    @GetMapping("/moyenne/etudiant/{etudiantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#etudiantId)")
    public ResponseEntity<Map<String, Object>> getMoyenneByEtudiant(@PathVariable Long etudiantId) {
        Double moyenne = noteService.calculateAverageByEtudiant(etudiantId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("etudiantId", etudiantId);
        response.put("moyenne", moyenne != null ? moyenne : "Aucune note");
        response.put("reussite", moyenne != null && moyenne >= 10);
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/notes/moyenne/cours/{coursId}
     * Calcule la moyenne d'un cours
     */
    @GetMapping("/moyenne/cours/{coursId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> getMoyenneByCours(@PathVariable Long coursId) {
        Double moyenne = noteService.calculateAverageByCours(coursId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("coursId", coursId);
        response.put("moyenne", moyenne != null ? moyenne : "Aucune note");
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/notes/count
     * Compte le nombre total de notes
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Long>> countNotes() {
        Map<String, Long> response = new HashMap<>();
        response.put("count", noteService.count());
        return ResponseEntity.ok(response);
    }

    /**
     * DTO pour la saisie de note
     */
    @lombok.Data
    public static class SaisieNoteRequest {
        private Long etudiantId;
        private Long coursId;
        private Double valeur;
        private String commentaire;
        private Long formateurId;
    }
}
