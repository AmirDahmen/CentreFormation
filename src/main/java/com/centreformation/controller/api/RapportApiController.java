package com.centreformation.controller.api;

import com.centreformation.entity.*;
import com.centreformation.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * API REST pour la génération de rapports PDF
 * 
 * Endpoints disponibles:
 * - GET /api/rapports/bulletin/{etudiantId}           - Bulletin de notes d'un étudiant
 * - GET /api/rapports/cours/{coursId}/etudiants       - Liste des étudiants d'un cours
 * - GET /api/rapports/cours/{coursId}/notes           - Relevé de notes d'un cours
 * - GET /api/rapports/statistiques                     - Rapport statistique global
 * - GET /api/rapports/attestation/{etudiantId}        - Attestation d'inscription
 */
@RestController
@RequestMapping("/api/rapports")
public class RapportApiController {
    
    private static final Logger logger = LoggerFactory.getLogger(RapportApiController.class);
    
    private final PdfService pdfService;
    private final EtudiantService etudiantService;
    private final CoursService coursService;
    private final NoteService noteService;
    private final InscriptionService inscriptionService;
    private final FormateurService formateurService;
    
    public RapportApiController(PdfService pdfService, 
                                EtudiantService etudiantService,
                                CoursService coursService, 
                                NoteService noteService,
                                InscriptionService inscriptionService,
                                FormateurService formateurService) {
        this.pdfService = pdfService;
        this.etudiantService = etudiantService;
        this.coursService = coursService;
        this.noteService = noteService;
        this.inscriptionService = inscriptionService;
        this.formateurService = formateurService;
    }
    
    /**
     * GET /api/rapports/bulletin/{etudiantId}
     * Génère le bulletin de notes d'un étudiant
     */
    @GetMapping("/bulletin/{etudiantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR') or @securityService.isCurrentEtudiant(#etudiantId)")
    public ResponseEntity<byte[]> getBulletinNotes(@PathVariable Long etudiantId) {
        logger.info("Demande de bulletin de notes pour l'étudiant ID: {}", etudiantId);
        
        try {
            Etudiant etudiant = etudiantService.findById(etudiantId);
            if (etudiant == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<Note> notes = noteService.findByEtudiantId(etudiantId);
            Double moyenne = noteService.getMoyenneByEtudiant(etudiantId);
            
            byte[] pdfContent = pdfService.generateBulletinNotes(etudiant, notes, moyenne);
            
            String filename = String.format("bulletin_%s_%s.pdf", 
                    etudiant.getMatricule(), 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return createPdfResponse(pdfContent, filename);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du bulletin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/rapports/cours/{coursId}/etudiants
     * Génère la liste des étudiants inscrits à un cours
     */
    @GetMapping("/cours/{coursId}/etudiants")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<byte[]> getListeEtudiantsCours(@PathVariable Long coursId) {
        logger.info("Demande de liste des étudiants pour le cours ID: {}", coursId);
        
        try {
            Cours cours = coursService.findById(coursId);
            if (cours == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<Inscription> inscriptions = inscriptionService.findByCours(coursId);
            // Filtrer uniquement les inscriptions actives
            inscriptions = inscriptions.stream()
                    .filter(i -> i.getStatut() == Inscription.StatutInscription.ACTIVE)
                    .toList();
            
            byte[] pdfContent = pdfService.generateListeEtudiantsCours(cours, inscriptions);
            
            String filename = String.format("liste_etudiants_%s_%s.pdf", 
                    cours.getCode(), 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return createPdfResponse(pdfContent, filename);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération de la liste des étudiants", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/rapports/cours/{coursId}/notes
     * Génère le relevé de notes d'un cours
     */
    @GetMapping("/cours/{coursId}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'FORMATEUR')")
    public ResponseEntity<byte[]> getReleveNotesCours(@PathVariable Long coursId) {
        logger.info("Demande de relevé de notes pour le cours ID: {}", coursId);
        
        try {
            Cours cours = coursService.findById(coursId);
            if (cours == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<Note> notes = noteService.findByCoursId(coursId);
            Double moyenne = noteService.getMoyenneByCours(coursId);
            Double tauxReussite = noteService.getTauxReussiteByCours(coursId);
            
            byte[] pdfContent = pdfService.generateReleveNotesCours(cours, notes, moyenne, tauxReussite);
            
            String filename = String.format("releve_notes_%s_%s.pdf", 
                    cours.getCode(), 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return createPdfResponse(pdfContent, filename);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du relevé de notes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/rapports/statistiques
     * Génère le rapport statistique global
     */
    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> getRapportStatistiques() {
        logger.info("Demande de rapport statistique global");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Statistiques de base
            stats.put("totalEtudiants", etudiantService.count());
            stats.put("totalFormateurs", formateurService.count());
            stats.put("totalCours", coursService.count());
            stats.put("inscriptionsActives", inscriptionService.countByStatut(
                    Inscription.StatutInscription.ACTIVE));
            stats.put("totalNotes", noteService.count());
            
            // Moyenne générale
            List<Note> allNotes = noteService.findAll();
            if (!allNotes.isEmpty()) {
                double moyenneGenerale = allNotes.stream()
                        .mapToDouble(Note::getValeur)
                        .average()
                        .orElse(0.0);
                stats.put("moyenneGenerale", moyenneGenerale);
                
                // Taux de réussite global
                long reussites = allNotes.stream()
                        .filter(n -> n.getValeur() >= 10)
                        .count();
                double tauxReussite = (reussites * 100.0) / allNotes.size();
                stats.put("tauxReussiteGlobal", tauxReussite);
            }
            
            // Cours populaires (top 5)
            List<Map<String, Object>> coursPopulaires = new ArrayList<>();
            List<Cours> allCours = coursService.findAll();
            
            allCours.stream()
                    .sorted((c1, c2) -> {
                        int count1 = inscriptionService.findByCours(c1.getId()).size();
                        int count2 = inscriptionService.findByCours(c2.getId()).size();
                        return Integer.compare(count2, count1);
                    })
                    .limit(5)
                    .forEach(cours -> {
                        Map<String, Object> cp = new HashMap<>();
                        cp.put("titre", cours.getTitre());
                        cp.put("nombreInscrits", inscriptionService.findByCours(cours.getId()).size());
                        coursPopulaires.add(cp);
                    });
            
            stats.put("coursPopulaires", coursPopulaires);
            
            byte[] pdfContent = pdfService.generateRapportStatistiques(stats);
            
            String filename = String.format("rapport_statistiques_%s.pdf", 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return createPdfResponse(pdfContent, filename);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du rapport statistique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/rapports/attestation/{etudiantId}
     * Génère l'attestation d'inscription d'un étudiant
     */
    @GetMapping("/attestation/{etudiantId}")
    @PreAuthorize("hasAnyRole('ADMIN') or @securityService.isCurrentEtudiant(#etudiantId)")
    public ResponseEntity<byte[]> getAttestationInscription(@PathVariable Long etudiantId) {
        logger.info("Demande d'attestation d'inscription pour l'étudiant ID: {}", etudiantId);
        
        try {
            Etudiant etudiant = etudiantService.findById(etudiantId);
            if (etudiant == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<Inscription> inscriptions = inscriptionService.findByEtudiant(etudiantId);
            // Filtrer uniquement les inscriptions actives
            inscriptions = inscriptions.stream()
                    .filter(i -> i.getStatut() == Inscription.StatutInscription.ACTIVE)
                    .toList();
            
            byte[] pdfContent = pdfService.generateAttestationInscription(etudiant, inscriptions);
            
            String filename = String.format("attestation_%s_%s.pdf", 
                    etudiant.getMatricule(), 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return createPdfResponse(pdfContent, filename);
            
        } catch (Exception e) {
            logger.error("Erreur lors de la génération de l'attestation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Crée la réponse HTTP avec le contenu PDF
     */
    private ResponseEntity<byte[]> createPdfResponse(byte[] pdfContent, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        headers.setContentLength(pdfContent.length);
        
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}
