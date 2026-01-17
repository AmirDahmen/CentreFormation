package com.centreformation.controller.api;

import com.centreformation.entity.SeanceCours;
import com.centreformation.service.SeanceCoursService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API REST pour la gestion des séances de cours et emplois du temps
 * 
 * Endpoints:
 * - GET    /api/seances                    - Liste toutes les séances (paginé)
 * - GET    /api/seances/{id}               - Détails d'une séance
 * - POST   /api/seances                    - Créer une séance (ADMIN/FORMATEUR)
 * - PUT    /api/seances/{id}               - Modifier une séance (ADMIN/FORMATEUR)
 * - DELETE /api/seances/{id}               - Supprimer une séance (ADMIN)
 * - GET    /api/seances/emploi-temps/etudiant/{id} - Emploi du temps étudiant
 * - GET    /api/seances/emploi-temps/formateur/{id} - Emploi du temps formateur
 * - GET    /api/seances/check-conflit      - Vérifier les conflits d'horaire
 */
@RestController
@RequestMapping("/api/seances")
@RequiredArgsConstructor
public class SeanceCoursApiController {

    private final SeanceCoursService seanceCoursService;

    /**
     * GET /api/seances
     * Liste toutes les séances avec pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Page<SeanceCours>> getAllSeances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") 
            ? Sort.by(sortBy).descending() 
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SeanceCours> seances = seanceCoursService.findAll(pageable);
        return ResponseEntity.ok(seances);
    }

    /**
     * GET /api/seances/all
     * Liste toutes les séances sans pagination
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<SeanceCours>> getAllSeancesList() {
        List<SeanceCours> seances = seanceCoursService.findAll();
        return ResponseEntity.ok(seances);
    }

    /**
     * GET /api/seances/{id}
     * Récupère une séance par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<SeanceCours> getSeanceById(@PathVariable Long id) {
        SeanceCours seance = seanceCoursService.findById(id);
        return ResponseEntity.ok(seance);
    }

    /**
     * POST /api/seances
     * Crée une nouvelle séance (vérifie les conflits d'horaire)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<SeanceCours> createSeance(@Valid @RequestBody SeanceCours seance) {
        SeanceCours created = seanceCoursService.save(seance);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/seances/{id}
     * Modifie une séance existante
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<SeanceCours> updateSeance(
            @PathVariable Long id, 
            @Valid @RequestBody SeanceCours seance) {
        SeanceCours updated = seanceCoursService.update(id, seance);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/seances/{id}
     * Supprime une séance (ADMIN uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteSeance(@PathVariable Long id) {
        seanceCoursService.deleteById(id);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Séance supprimée avec succès");
        response.put("id", id.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/seances/date/{date}
     * Liste les séances d'une date donnée
     */
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<SeanceCours>> getSeancesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SeanceCours> seances = seanceCoursService.findByDate(date);
        return ResponseEntity.ok(seances);
    }

    /**
     * GET /api/seances/periode
     * Liste les séances pour une période donnée
     */
    @GetMapping("/periode")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<SeanceCours>> getSeancesByPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        List<SeanceCours> seances = seanceCoursService.findByDateBetween(debut, fin);
        return ResponseEntity.ok(seances);
    }

    /**
     * GET /api/seances/cours/{coursId}
     * Liste les séances d'un cours
     */
    @GetMapping("/cours/{coursId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<SeanceCours>> getSeancesByCours(@PathVariable Long coursId) {
        List<SeanceCours> seances = seanceCoursService.findByCours(coursId);
        return ResponseEntity.ok(seances);
    }

    /**
     * GET /api/seances/formateur/{formateurId}
     * Liste les séances d'un formateur
     */
    @GetMapping("/formateur/{formateurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<SeanceCours>> getSeancesByFormateur(@PathVariable Long formateurId) {
        List<SeanceCours> seances = seanceCoursService.findByFormateur(formateurId);
        return ResponseEntity.ok(seances);
    }

    /**
     * GET /api/seances/groupe/{groupeId}
     * Liste les séances d'un groupe
     */
    @GetMapping("/groupe/{groupeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR', 'ETUDIANT')")
    public ResponseEntity<List<SeanceCours>> getSeancesByGroupe(@PathVariable Long groupeId) {
        List<SeanceCours> seances = seanceCoursService.findByGroupe(groupeId);
        return ResponseEntity.ok(seances);
    }

    /**
     * GET /api/seances/emploi-temps/etudiant/{etudiantId}
     * Récupère l'emploi du temps d'un étudiant pour une période
     */
    @GetMapping("/emploi-temps/etudiant/{etudiantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#etudiantId)")
    public ResponseEntity<List<SeanceCours>> getEmploiTempsEtudiant(
            @PathVariable Long etudiantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        
        // Par défaut : semaine courante
        if (debut == null) {
            debut = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        }
        if (fin == null) {
            fin = debut.plusDays(6);
        }
        
        List<SeanceCours> seances = seanceCoursService.getEmploiTempsEtudiant(etudiantId, debut, fin);
        return ResponseEntity.ok(seances);
    }

    /**
     * GET /api/seances/emploi-temps/formateur/{formateurId}
     * Récupère l'emploi du temps d'un formateur pour une période
     */
    @GetMapping("/emploi-temps/formateur/{formateurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<SeanceCours>> getEmploiTempsFormateur(
            @PathVariable Long formateurId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        
        // Par défaut : semaine courante
        if (debut == null) {
            debut = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        }
        if (fin == null) {
            fin = debut.plusDays(6);
        }
        
        List<SeanceCours> seances = seanceCoursService.getEmploiTempsFormateur(formateurId, debut, fin);
        return ResponseEntity.ok(seances);
    }

    /**
     * GET /api/seances/check-conflit
     * Vérifie s'il y a un conflit d'horaire
     */
    @GetMapping("/check-conflit")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> checkConflit(
            @RequestParam(required = false) Long formateurId,
            @RequestParam(required = false) Long groupeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime heureDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime heureFin,
            @RequestParam(required = false) Long excludeSeanceId) {
        
        Map<String, Object> response = new HashMap<>();
        boolean hasConflit = false;
        String message = "Aucun conflit détecté";
        
        if (formateurId != null) {
            if (seanceCoursService.hasConflitFormateur(formateurId, date, heureDebut, heureFin, excludeSeanceId)) {
                hasConflit = true;
                message = "Le formateur a déjà une séance à ce créneau";
            }
        }
        
        if (!hasConflit && groupeId != null) {
            if (seanceCoursService.hasConflitGroupe(groupeId, date, heureDebut, heureFin, excludeSeanceId)) {
                hasConflit = true;
                message = "Le groupe a déjà une séance à ce créneau";
            }
        }
        
        response.put("hasConflit", hasConflit);
        response.put("message", message);
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/seances/count
     * Compte le nombre total de séances
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Long>> countSeances() {
        Map<String, Long> response = new HashMap<>();
        response.put("count", seanceCoursService.count());
        return ResponseEntity.ok(response);
    }
}
