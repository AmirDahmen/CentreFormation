package com.centreformation.controller.api;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Inscription;
import com.centreformation.entity.Note;
import com.centreformation.service.CoursService;
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
 * API REST pour la gestion des cours
 * 
 * Endpoints:
 * - GET    /api/cours          - Liste tous les cours (paginé)
 * - GET    /api/cours/{id}     - Détails d'un cours
 * - POST   /api/cours          - Créer un cours (ADMIN)
 * - PUT    /api/cours/{id}     - Modifier un cours (ADMIN/FORMATEUR)
 * - DELETE /api/cours/{id}     - Supprimer un cours (ADMIN)
 * - GET    /api/cours/{id}/inscriptions - Inscriptions d'un cours
 * - GET    /api/cours/{id}/notes        - Notes d'un cours
 * - GET    /api/cours/{id}/moyenne      - Moyenne d'un cours
 * - GET    /api/cours/{id}/taux-reussite - Taux de réussite d'un cours
 */
@RestController
@RequestMapping("/api/cours")
@RequiredArgsConstructor
public class CoursApiController {

    private final CoursService coursService;
    private final InscriptionService inscriptionService;
    private final NoteService noteService;

    /**
     * GET /api/cours
     * Liste tous les cours avec pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<Page<Cours>> getAllCours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "titre") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Cours> cours = coursService.findAll(pageable);
        return ResponseEntity.ok(cours);
    }

    /**
     * GET /api/cours/all
     * Liste tous les cours sans pagination (pour les selects)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<List<Cours>> getAllCoursList() {
        List<Cours> cours = coursService.findAll();
        return ResponseEntity.ok(cours);
    }

    /**
     * GET /api/cours/{id}
     * Récupère un cours par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<Cours> getCoursById(@PathVariable Long id) {
        Cours cours = coursService.findById(id);
        return ResponseEntity.ok(cours);
    }

    /**
     * GET /api/cours/code/{code}
     * Récupère un cours par son code
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Cours> getCoursByCode(@PathVariable String code) {
        Cours cours = coursService.findByCode(code);
        return ResponseEntity.ok(cours);
    }

    /**
     * POST /api/cours
     * Crée un nouveau cours (ADMIN uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Cours> createCours(@Valid @RequestBody Cours cours) {
        Cours created = coursService.save(cours);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/cours/{id}
     * Modifie un cours existant (ADMIN ou FORMATEUR du cours)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isFormateurOfCours(#id)")
    public ResponseEntity<Cours> updateCours(
            @PathVariable Long id, 
            @Valid @RequestBody Cours cours) {
        Cours updated = coursService.update(id, cours);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/cours/{id}
     * Supprime un cours (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCours(@PathVariable Long id) {
        coursService.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cours supprimé avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cours/search
     * Recherche des cours par mot-clé
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<Page<Cours>> searchCours(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Cours> cours = coursService.search(keyword, pageable);
        return ResponseEntity.ok(cours);
    }

    /**
     * GET /api/cours/formateur/{formateurId}
     * Liste les cours par formateur
     */
    @GetMapping("/formateur/{formateurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<Cours>> getCoursByFormateur(
            @PathVariable Long formateurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Cours> cours = coursService.findByFormateur(formateurId, pageable);
        return ResponseEntity.ok(cours);
    }

    /**
     * GET /api/cours/groupe/{groupeId}
     * Liste les cours par groupe
     */
    @GetMapping("/groupe/{groupeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<List<Cours>> getCoursByGroupe(@PathVariable Long groupeId) {
        List<Cours> cours = coursService.findByGroupe(groupeId);
        return ResponseEntity.ok(cours);
    }

    /**
     * GET /api/cours/{id}/inscriptions
     * Récupère les inscriptions d'un cours
     */
    @GetMapping("/{id}/inscriptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Inscription>> getInscriptionsByCours(@PathVariable Long id) {
        List<Inscription> inscriptions = inscriptionService.findByCours(id);
        return ResponseEntity.ok(inscriptions);
    }

    /**
     * GET /api/cours/{id}/notes
     * Récupère les notes d'un cours
     */
    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Note>> getNotesByCours(@PathVariable Long id) {
        List<Note> notes = noteService.findByCours(id);
        return ResponseEntity.ok(notes);
    }

    /**
     * GET /api/cours/{id}/moyenne
     * Calcule la moyenne d'un cours
     */
    @GetMapping("/{id}/moyenne")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> getMoyenneByCours(@PathVariable Long id) {
        Cours cours = coursService.findById(id);
        Double moyenne = noteService.calculateAverageByCours(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("coursId", id);
        response.put("coursTitre", cours.getTitre());
        response.put("moyenne", moyenne != null ? moyenne : "Aucune note");
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cours/{id}/taux-reussite
     * Calcule le taux de réussite d'un cours (notes >= 10)
     */
    @GetMapping("/{id}/taux-reussite")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> getTauxReussiteByCours(@PathVariable Long id) {
        Cours cours = coursService.findById(id);
        List<Note> notes = noteService.findByCours(id);
        
        long totalNotes = notes.size();
        long notesReussies = notes.stream()
                .filter(n -> n.getValeur() >= 10)
                .count();
        
        double tauxReussite = totalNotes > 0 
            ? (double) notesReussies / totalNotes * 100 
            : 0;
        
        Map<String, Object> response = new HashMap<>();
        response.put("coursId", id);
        response.put("coursTitre", cours.getTitre());
        response.put("totalEtudiants", totalNotes);
        response.put("etudiantsReussis", notesReussies);
        response.put("tauxReussite", Math.round(tauxReussite * 100.0) / 100.0);
        
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/cours/{coursId}/groupes/{groupeId}
     * Ajoute un groupe à un cours
     */
    @PutMapping("/{coursId}/groupes/{groupeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> addGroupeToCours(
            @PathVariable Long coursId,
            @PathVariable Long groupeId) {
        coursService.addGroupeToCours(coursId, groupeId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Groupe ajouté au cours avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/cours/{coursId}/groupes/{groupeId}
     * Retire un groupe d'un cours
     */
    @DeleteMapping("/{coursId}/groupes/{groupeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> removeGroupeFromCours(
            @PathVariable Long coursId,
            @PathVariable Long groupeId) {
        coursService.removeGroupeFromCours(coursId, groupeId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Groupe retiré du cours avec succès");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cours/count
     * Compte le nombre total de cours
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Long>> countCours() {
        Map<String, Long> response = new HashMap<>();
        response.put("count", coursService.count());
        return ResponseEntity.ok(response);
    }
}
