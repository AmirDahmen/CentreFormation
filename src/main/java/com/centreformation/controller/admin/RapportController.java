package com.centreformation.controller.admin;

import com.centreformation.entity.*;
import com.centreformation.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur pour la génération de rapports PDF depuis l'interface admin (SSR)
 */
@Controller
@RequestMapping("/admin/rapports")
public class RapportController {
    
    private static final Logger logger = LoggerFactory.getLogger(RapportController.class);
    
    private final PdfService pdfService;
    private final EtudiantService etudiantService;
    private final CoursService coursService;
    private final NoteService noteService;
    private final InscriptionService inscriptionService;
    private final FormateurService formateurService;
    
    public RapportController(PdfService pdfService, 
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
     * Page principale des rapports
     */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("etudiants", etudiantService.findAll());
        model.addAttribute("cours", coursService.findAll());
        return "admin/rapports/index";
    }
    
    /**
     * Télécharger le bulletin de notes d'un étudiant
     */
    @GetMapping("/bulletin/{etudiantId}")
    public ResponseEntity<byte[]> downloadBulletin(@PathVariable Long etudiantId) {
        logger.info("Téléchargement bulletin pour étudiant ID: {}", etudiantId);
        
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
            logger.error("Erreur génération bulletin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Télécharger la liste des étudiants d'un cours
     */
    @GetMapping("/cours/{coursId}/etudiants")
    public ResponseEntity<byte[]> downloadListeEtudiants(@PathVariable Long coursId) {
        logger.info("Téléchargement liste étudiants pour cours ID: {}", coursId);
        
        try {
            Cours cours = coursService.findById(coursId);
            if (cours == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<Inscription> inscriptions = inscriptionService.findByCours(coursId);
            inscriptions = inscriptions.stream()
                    .filter(i -> i.getStatut() == Inscription.StatutInscription.ACTIVE)
                    .toList();
            
            byte[] pdfContent = pdfService.generateListeEtudiantsCours(cours, inscriptions);
            
            String filename = String.format("liste_etudiants_%s_%s.pdf", 
                    cours.getCode(), 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return createPdfResponse(pdfContent, filename);
            
        } catch (Exception e) {
            logger.error("Erreur génération liste étudiants", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Télécharger le relevé de notes d'un cours
     */
    @GetMapping("/cours/{coursId}/notes")
    public ResponseEntity<byte[]> downloadReleveNotes(@PathVariable Long coursId) {
        logger.info("Téléchargement relevé notes pour cours ID: {}", coursId);
        
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
            logger.error("Erreur génération relevé notes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Télécharger le rapport statistique global
     */
    @GetMapping("/statistiques")
    public ResponseEntity<byte[]> downloadStatistiques() {
        logger.info("Téléchargement rapport statistiques");
        
        try {
            Map<String, Object> stats = buildStatistics();
            
            byte[] pdfContent = pdfService.generateRapportStatistiques(stats);
            
            String filename = String.format("rapport_statistiques_%s.pdf", 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return createPdfResponse(pdfContent, filename);
            
        } catch (Exception e) {
            logger.error("Erreur génération rapport statistiques", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Télécharger l'attestation d'inscription
     */
    @GetMapping("/attestation/{etudiantId}")
    public ResponseEntity<byte[]> downloadAttestation(@PathVariable Long etudiantId) {
        logger.info("Téléchargement attestation pour étudiant ID: {}", etudiantId);
        
        try {
            Etudiant etudiant = etudiantService.findById(etudiantId);
            if (etudiant == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<Inscription> inscriptions = inscriptionService.findByEtudiant(etudiantId);
            inscriptions = inscriptions.stream()
                    .filter(i -> i.getStatut() == Inscription.StatutInscription.ACTIVE)
                    .toList();
            
            byte[] pdfContent = pdfService.generateAttestationInscription(etudiant, inscriptions);
            
            String filename = String.format("attestation_%s_%s.pdf", 
                    etudiant.getMatricule(), 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return createPdfResponse(pdfContent, filename);
            
        } catch (Exception e) {
            logger.error("Erreur génération attestation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private Map<String, Object> buildStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalEtudiants", etudiantService.count());
        stats.put("totalFormateurs", formateurService.count());
        stats.put("totalCours", coursService.count());
        stats.put("inscriptionsActives", inscriptionService.countByStatut(
                Inscription.StatutInscription.ACTIVE));
        stats.put("totalNotes", noteService.count());
        
        List<Note> allNotes = noteService.findAll();
        if (!allNotes.isEmpty()) {
            double moyenneGenerale = allNotes.stream()
                    .mapToDouble(Note::getValeur)
                    .average()
                    .orElse(0.0);
            stats.put("moyenneGenerale", moyenneGenerale);
            
            long reussites = allNotes.stream()
                    .filter(n -> n.getValeur() >= 10)
                    .count();
            double tauxReussite = (reussites * 100.0) / allNotes.size();
            stats.put("tauxReussiteGlobal", tauxReussite);
        }
        
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
        
        return stats;
    }
    
    private ResponseEntity<byte[]> createPdfResponse(byte[] pdfContent, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        headers.setContentLength(pdfContent.length);
        
        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }
}
