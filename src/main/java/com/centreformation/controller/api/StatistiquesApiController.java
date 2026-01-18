package com.centreformation.controller.api;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Inscription;
import com.centreformation.entity.Note;
import com.centreformation.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * API REST pour les statistiques et le dashboard
 * 
 * Endpoints:
 * - GET /api/stats/dashboard           - Statistiques générales du dashboard
 * - GET /api/stats/cours-populaires    - Cours les plus suivis
 * - GET /api/stats/taux-reussite       - Taux de réussite global
 * - GET /api/stats/inscriptions        - Statistiques des inscriptions
 * - GET /api/stats/notes               - Statistiques des notes
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatistiquesApiController {

    private final EtudiantService etudiantService;
    private final FormateurService formateurService;
    private final CoursService coursService;
    private final InscriptionService inscriptionService;
    private final NoteService noteService;
    private final GroupeService groupeService;
    private final SpecialiteService specialiteService;
    private final SessionPedagogiqueService sessionService;

    /**
     * GET /api/stats/dashboard
     * Statistiques générales pour le dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Compteurs généraux
        stats.put("totalEtudiants", etudiantService.count());
        stats.put("totalFormateurs", formateurService.count());
        stats.put("totalCours", coursService.count());
        stats.put("totalGroupes", groupeService.count());
        stats.put("totalSpecialites", specialiteService.count());
        
        // Statistiques inscriptions
        stats.put("inscriptionsValidees", inscriptionService.countByStatut(Inscription.StatutInscription.VALIDEE));
        stats.put("inscriptionsEnAttente", inscriptionService.countByStatut(Inscription.StatutInscription.EN_ATTENTE));
        stats.put("inscriptionsRefusees", inscriptionService.countByStatut(Inscription.StatutInscription.REFUSEE));
        stats.put("inscriptionsAnnulees", inscriptionService.countByStatut(Inscription.StatutInscription.ANNULEE));
        stats.put("totalInscriptions", inscriptionService.count());
        
        // Statistiques notes
        stats.put("totalNotes", noteService.count());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/stats/cours-populaires
     * Liste des cours les plus suivis (par nombre d'inscriptions)
     */
    @GetMapping("/cours-populaires")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<List<Map<String, Object>>> getCoursPopulaires(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<Cours> allCours = coursService.findAll();
        
        List<Map<String, Object>> coursStats = allCours.stream()
                .map(cours -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("coursId", cours.getId());
                    stat.put("code", cours.getCode());
                    stat.put("titre", cours.getTitre());
                    stat.put("formateur", cours.getFormateur() != null 
                            ? cours.getFormateur().getNom() + " " + cours.getFormateur().getPrenom() 
                            : "Non assigné");
                    
                    List<Inscription> inscriptions = inscriptionService.findByCours(cours.getId());
                    long inscriptionsValidees = inscriptions.stream()
                            .filter(i -> i.getStatut() == Inscription.StatutInscription.VALIDEE)
                            .count();
                    stat.put("nombreInscrits", inscriptionsValidees);
                    stat.put("totalInscriptions", inscriptions.size());
                    
                    Double moyenne = noteService.calculateAverageByCours(cours.getId());
                    stat.put("moyenneCours", moyenne != null ? Math.round(moyenne * 100.0) / 100.0 : null);
                    
                    return stat;
                })
                .sorted((a, b) -> Long.compare(
                        (Long) b.get("nombreInscrits"), 
                        (Long) a.get("nombreInscrits")))
                .limit(limit)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(coursStats);
    }

    /**
     * GET /api/stats/taux-reussite
     * Taux de réussite global et par cours
     */
    @GetMapping("/taux-reussite")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> getTauxReussite() {
        List<Note> allNotes = noteService.findAll();
        
        long totalNotes = allNotes.size();
        long notesReussies = allNotes.stream()
                .filter(n -> n.getValeur() >= 10)
                .count();
        
        double tauxGlobal = totalNotes > 0 
                ? (double) notesReussies / totalNotes * 100 
                : 0;
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalNotes", totalNotes);
        response.put("notesReussies", notesReussies);
        response.put("notesEchouees", totalNotes - notesReussies);
        response.put("tauxReussiteGlobal", Math.round(tauxGlobal * 100.0) / 100.0);
        
        // Taux par cours
        List<Cours> allCours = coursService.findAll();
        List<Map<String, Object>> tauxParCours = allCours.stream()
                .map(cours -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("coursId", cours.getId());
                    stat.put("coursTitre", cours.getTitre());
                    
                    List<Note> notesCours = noteService.findByCours(cours.getId());
                    long total = notesCours.size();
                    long reussies = notesCours.stream()
                            .filter(n -> n.getValeur() >= 10)
                            .count();
                    double taux = total > 0 ? (double) reussies / total * 100 : 0;
                    
                    stat.put("totalNotes", total);
                    stat.put("notesReussies", reussies);
                    stat.put("tauxReussite", Math.round(taux * 100.0) / 100.0);
                    
                    return stat;
                })
                .filter(stat -> (Long) stat.get("totalNotes") > 0)
                .sorted((a, b) -> Double.compare(
                        (Double) b.get("tauxReussite"), 
                        (Double) a.get("tauxReussite")))
                .collect(Collectors.toList());
        
        response.put("tauxParCours", tauxParCours);
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/stats/inscriptions
     * Statistiques détaillées des inscriptions
     */
    @GetMapping("/inscriptions")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> getInscriptionsStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long total = inscriptionService.count();
        long validees = inscriptionService.countByStatut(Inscription.StatutInscription.VALIDEE);
        long enAttente = inscriptionService.countByStatut(Inscription.StatutInscription.EN_ATTENTE);
        long refusees = inscriptionService.countByStatut(Inscription.StatutInscription.REFUSEE);
        long annulees = inscriptionService.countByStatut(Inscription.StatutInscription.ANNULEE);
        
        stats.put("total", total);
        stats.put("validees", validees);
        stats.put("enAttente", enAttente);
        stats.put("refusees", refusees);
        stats.put("annulees", annulees);
        
        // Pourcentages
        stats.put("pourcentageValidees", total > 0 ? Math.round((double) validees / total * 10000.0) / 100.0 : 0);
        stats.put("pourcentageEnAttente", total > 0 ? Math.round((double) enAttente / total * 10000.0) / 100.0 : 0);
        stats.put("pourcentageRefusees", total > 0 ? Math.round((double) refusees / total * 10000.0) / 100.0 : 0);
        stats.put("pourcentageAnnulees", total > 0 ? Math.round((double) annulees / total * 10000.0) / 100.0 : 0);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/stats/notes
     * Statistiques détaillées des notes
     */
    @GetMapping("/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> getNotesStats() {
        List<Note> allNotes = noteService.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNotes", allNotes.size());
        
        if (allNotes.isEmpty()) {
            stats.put("moyenne", null);
            stats.put("min", null);
            stats.put("max", null);
            stats.put("distribution", Collections.emptyMap());
            return ResponseEntity.ok(stats);
        }
        
        // Statistiques basiques
        double moyenne = allNotes.stream()
                .mapToDouble(Note::getValeur)
                .average()
                .orElse(0);
        double min = allNotes.stream()
                .mapToDouble(Note::getValeur)
                .min()
                .orElse(0);
        double max = allNotes.stream()
                .mapToDouble(Note::getValeur)
                .max()
                .orElse(0);
        
        stats.put("moyenne", Math.round(moyenne * 100.0) / 100.0);
        stats.put("min", min);
        stats.put("max", max);
        
        // Distribution par tranches
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("0-5", allNotes.stream().filter(n -> n.getValeur() < 5).count());
        distribution.put("5-8", allNotes.stream().filter(n -> n.getValeur() >= 5 && n.getValeur() < 8).count());
        distribution.put("8-10", allNotes.stream().filter(n -> n.getValeur() >= 8 && n.getValeur() < 10).count());
        distribution.put("10-12", allNotes.stream().filter(n -> n.getValeur() >= 10 && n.getValeur() < 12).count());
        distribution.put("12-14", allNotes.stream().filter(n -> n.getValeur() >= 12 && n.getValeur() < 14).count());
        distribution.put("14-16", allNotes.stream().filter(n -> n.getValeur() >= 14 && n.getValeur() < 16).count());
        distribution.put("16-18", allNotes.stream().filter(n -> n.getValeur() >= 16 && n.getValeur() < 18).count());
        distribution.put("18-20", allNotes.stream().filter(n -> n.getValeur() >= 18).count());
        
        stats.put("distribution", distribution);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/stats/etudiant/{id}
     * Statistiques pour un étudiant spécifique
     */
    @GetMapping("/etudiant/{etudiantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#etudiantId)")
    public ResponseEntity<Map<String, Object>> getEtudiantStats(@PathVariable Long etudiantId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Informations étudiant
        var etudiant = etudiantService.findById(etudiantId);
        stats.put("etudiantId", etudiant.getId());
        stats.put("matricule", etudiant.getMatricule());
        stats.put("nom", etudiant.getNom() + " " + etudiant.getPrenom());
        
        // Inscriptions
        var inscriptions = inscriptionService.findByEtudiant(etudiantId);
        stats.put("totalInscriptions", inscriptions.size());
        stats.put("inscriptionsValidees", inscriptions.stream()
                .filter(i -> i.getStatut() == Inscription.StatutInscription.VALIDEE)
                .count());
        
        // Notes
        var notes = noteService.findByEtudiant(etudiantId);
        stats.put("totalNotes", notes.size());
        
        Double moyenne = noteService.calculateAverageByEtudiant(etudiantId);
        stats.put("moyenneGenerale", moyenne != null ? Math.round(moyenne * 100.0) / 100.0 : null);
        stats.put("reussite", moyenne != null && moyenne >= 10);
        
        // Détail des notes par cours
        List<Map<String, Object>> notesDetail = notes.stream()
                .map(note -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("coursId", note.getCours().getId());
                    detail.put("coursTitre", note.getCours().getTitre());
                    detail.put("valeur", note.getValeur());
                    detail.put("reussite", note.getValeur() >= 10);
                    return detail;
                })
                .collect(Collectors.toList());
        stats.put("notes", notesDetail);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/stats/formateur/{id}
     * Statistiques pour un formateur spécifique
     */
    @GetMapping("/formateur/{formateurId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<Map<String, Object>> getFormateurStats(@PathVariable Long formateurId) {
        Map<String, Object> stats = new HashMap<>();
        
        // Informations formateur
        var formateur = formateurService.findById(formateurId);
        stats.put("formateurId", formateur.getId());
        stats.put("nom", formateur.getNom() + " " + formateur.getPrenom());
        stats.put("email", formateur.getEmail());
        
        // Cours
        var cours = formateurService.findCoursByFormateur(formateurId);
        stats.put("nombreCours", cours.size());
        
        // Statistiques sur les cours
        List<Map<String, Object>> coursStats = cours.stream()
                .map(c -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("coursId", c.getId());
                    stat.put("titre", c.getTitre());
                    
                    var inscriptions = inscriptionService.findByCours(c.getId());
                    stat.put("nombreInscrits", inscriptions.stream()
                            .filter(i -> i.getStatut() == Inscription.StatutInscription.VALIDEE)
                            .count());
                    
                    Double moyenne = noteService.calculateAverageByCours(c.getId());
                    stat.put("moyenne", moyenne != null ? Math.round(moyenne * 100.0) / 100.0 : null);
                    
                    return stat;
                })
                .collect(Collectors.toList());
        stats.put("cours", coursStats);
        
        // Total étudiants sous ce formateur
        long totalEtudiants = cours.stream()
                .flatMap(c -> inscriptionService.findByCours(c.getId()).stream())
                .filter(i -> i.getStatut() == Inscription.StatutInscription.VALIDEE)
                .map(i -> i.getEtudiant().getId())
                .distinct()
                .count();
        stats.put("totalEtudiants", totalEtudiants);
        
        return ResponseEntity.ok(stats);
    }
}
