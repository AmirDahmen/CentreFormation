package com.centreformation.controller.admin;

import com.centreformation.entity.*;
import com.centreformation.repository.SalleRepository;
import com.centreformation.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.*;

@Controller
@RequestMapping("/admin/seances")
@RequiredArgsConstructor
public class SeanceController {

    private final SeanceCoursService seanceCoursService;
    private final CoursService coursService;
    private final FormateurService formateurService;
    private final GroupeService groupeService;
    private final SpecialiteService specialiteService;
    private final SalleRepository salleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Étape 1: Page d'accueil planification - Liste des spécialités
     */
    @GetMapping
    public String planificationHome(Model model) {
        List<Specialite> specialites = specialiteService.findAll();
        model.addAttribute("specialites", specialites);
        model.addAttribute("activePage", "seances");
        model.addAttribute("etape", 1);
        return "admin/seances/planification";
    }

    /**
     * Étape 2: Liste des groupes d'une spécialité
     */
    @GetMapping("/specialite/{specialiteId}")
    public String selectGroupe(@PathVariable Long specialiteId, Model model) {
        Specialite specialite = specialiteService.findById(specialiteId);
        List<Groupe> groupes = groupeService.findBySpecialite(specialiteId);
        
        model.addAttribute("specialite", specialite);
        model.addAttribute("groupes", groupes);
        model.addAttribute("activePage", "seances");
        model.addAttribute("etape", 2);
        return "admin/seances/planification";
    }

    /**
     * Étape 3: Grille de planification pour un groupe spécifique
     */
    @GetMapping("/groupe/{groupeId}")
    public String planificationGroupe(
            @PathVariable Long groupeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        
        Groupe groupe = groupeService.findById(groupeId);
        if (groupe == null) {
            return "redirect:/admin/seances";
        }
        
        // Date par défaut = aujourd'hui
        if (date == null) {
            date = LocalDate.now();
        }
        
        // Calculer le début et fin de semaine (lundi = jour 1)
        LocalDate startOfWeek = date.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 1);
        LocalDate endOfWeek = startOfWeek.plusDays(5); // Samedi inclus
        
        // Créer le tableau des jours de la semaine (lundi à samedi)
        List<LocalDate> joursSemaine = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            joursSemaine.add(startOfWeek.plusDays(i));
        }
        
        // Récupérer les séances du groupe pour cette semaine
        List<SeanceCours> seances = seanceCoursService.findByGroupeAndDateBetween(groupeId, startOfWeek, endOfWeek);
        
        // Convertir les séances en JSON pour le JavaScript
        String seancesJson = convertSeancesToJson(seances);
        
        // NOUVEAU: Le groupe appartient à UN seul cours
        Cours coursGroupe = groupe.getCours();
        
        // Liste des formateurs pour le modal
        List<Formateur> formateurs = formateurService.findAll();
        
        // Liste de toutes les salles actives
        List<Salle> salles = salleRepository.findByActifTrueOrderByCodeAsc();
        
        model.addAttribute("groupe", groupe);
        model.addAttribute("specialite", groupe.getSpecialite());
        model.addAttribute("joursSemaine", joursSemaine);
        model.addAttribute("seancesJson", seancesJson);
        model.addAttribute("seances", seances);
        model.addAttribute("coursGroupe", coursGroupe);
        model.addAttribute("formateurs", formateurs);
        model.addAttribute("salles", salles);
        model.addAttribute("currentDate", date);
        model.addAttribute("startOfWeek", startOfWeek);
        model.addAttribute("endOfWeek", endOfWeek);
        model.addAttribute("activePage", "seances");
        model.addAttribute("etape", 3);
        
        return "admin/seances/planification";
    }
    
    /**
     * Convertit les séances en JSON pour le rendu JavaScript
     */
    private String convertSeancesToJson(List<SeanceCours> seances) {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (SeanceCours s : seances) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getId());
                map.put("date", s.getDate().toString());
                map.put("heureDebut", s.getHeureDebut().getHour());
                map.put("heureFin", s.getHeureFin().getHour());
                // Salle: priorité à l'entité Salle, sinon le texte
                if (s.getSalle() != null) {
                    map.put("salleId", s.getSalle().getId());
                    map.put("salle", s.getSalle().getCode());
                } else if (s.getSalleTexte() != null) {
                    map.put("salleId", null);
                    map.put("salle", s.getSalleTexte());
                } else {
                    map.put("salleId", null);
                    map.put("salle", "");
                }
                map.put("coursId", s.getCours() != null ? s.getCours().getId() : null);
                map.put("coursCode", s.getCours() != null ? s.getCours().getCode() : "");
                map.put("coursNom", s.getCours() != null ? s.getCours().getTitre() : "");
                map.put("formateurId", s.getFormateur() != null ? s.getFormateur().getId() : null);
                map.put("formateurNom", s.getFormateur() != null ? s.getFormateur().getNom() + " " + s.getFormateur().getPrenom() : "");
                map.put("groupeId", s.getGroupe() != null ? s.getGroupe().getId() : null);
                map.put("groupeNom", s.getGroupe() != null ? s.getGroupe().getNom() : "");
                list.add(map);
            }
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    // ===================== API AJAX pour Drag & Drop =====================

    /**
     * API: Créer une séance via drag & drop
     */
    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<?> createSeanceApi(@RequestBody Map<String, Object> payload) {
        try {
            SeanceCours seance = new SeanceCours();
            
            LocalDate date = LocalDate.parse((String) payload.get("date"));
            LocalTime heureDebut = LocalTime.parse((String) payload.get("heureDebut"));
            LocalTime heureFin = LocalTime.parse((String) payload.get("heureFin"));
            
            seance.setDate(date);
            seance.setHeureDebut(heureDebut);
            seance.setHeureFin(heureFin);
            
            Long coursId = Long.valueOf(payload.get("coursId").toString());
            seance.setCours(coursService.findById(coursId));
            
            Long groupeId = Long.valueOf(payload.get("groupeId").toString());
            Groupe groupe = groupeService.findById(groupeId);
            seance.setGroupe(groupe);
            
            // Vérifier conflit groupe
            if (seanceCoursService.hasConflitGroupe(groupeId, seance.getDate(), 
                    seance.getHeureDebut(), seance.getHeureFin(), null)) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Conflit: le groupe a déjà cours sur ce créneau"
                ));
            }
            
            // Gestion de la salle
            if (payload.get("salleId") != null && !payload.get("salleId").toString().isEmpty()) {
                Long salleId = Long.valueOf(payload.get("salleId").toString());
                Salle salle = salleRepository.findById(salleId).orElse(null);
                if (salle != null) {
                    // Vérifier si la salle est disponible
                    if (!salleRepository.isSalleDisponible(salleId, date, heureDebut, heureFin)) {
                        return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "Conflit: la salle " + salle.getCode() + " est déjà occupée sur ce créneau"
                        ));
                    }
                    seance.setSalle(salle);
                }
            }
            
            if (payload.get("formateurId") != null && !payload.get("formateurId").toString().isEmpty()) {
                Long formateurId = Long.valueOf(payload.get("formateurId").toString());
                
                // Vérifier conflit formateur
                if (seanceCoursService.hasConflitFormateur(formateurId, seance.getDate(), 
                        seance.getHeureDebut(), seance.getHeureFin(), null)) {
                    return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Conflit: le formateur est déjà occupé sur ce créneau"
                    ));
                }
                seance.setFormateur(formateurService.findById(formateurId));
            }
            
            SeanceCours saved = seanceCoursService.save(seance);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Séance créée avec succès",
                "seance", Map.of(
                    "id", saved.getId(),
                    "date", saved.getDate().toString(),
                    "heureDebut", saved.getHeureDebut().getHour(),
                    "heureFin", saved.getHeureFin().getHour(),
                    "coursCode", saved.getCours().getCode(),
                    "coursNom", saved.getCours().getTitre(),
                    "salle", saved.getSalle() != null ? saved.getSalle().getCode() : "",
                    "formateurNom", saved.getFormateur() != null ? 
                        saved.getFormateur().getNom() + " " + saved.getFormateur().getPrenom() : ""
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    /**
     * API: Modifier/Déplacer une séance
     */
    @PostMapping("/api/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateSeanceApi(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        try {
            SeanceCours seance = seanceCoursService.findById(id);
            if (seance == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Séance introuvable"));
            }
            
            LocalDate newDate = LocalDate.parse((String) payload.get("date"));
            LocalTime newHeureDebut = LocalTime.parse((String) payload.get("heureDebut"));
            LocalTime newHeureFin = LocalTime.parse((String) payload.get("heureFin"));
            
            // Vérifier conflit groupe
            if (seance.getGroupe() != null && seanceCoursService.hasConflitGroupe(
                    seance.getGroupe().getId(), newDate, newHeureDebut, newHeureFin, id)) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Conflit: le groupe a déjà cours sur ce créneau"
                ));
            }
            
            // Vérifier conflit formateur
            if (seance.getFormateur() != null && seanceCoursService.hasConflitFormateur(
                    seance.getFormateur().getId(), newDate, newHeureDebut, newHeureFin, id)) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Conflit: le formateur est déjà occupé sur ce créneau"
                ));
            }
            
            seance.setDate(newDate);
            seance.setHeureDebut(newHeureDebut);
            seance.setHeureFin(newHeureFin);
            
            // Gestion de la salle (entité)
            if (payload.get("salleId") != null && !payload.get("salleId").toString().isEmpty()) {
                Long salleId = Long.valueOf(payload.get("salleId").toString());
                Salle salle = salleRepository.findById(salleId).orElse(null);
                if (salle != null) {
                    // Vérifier disponibilité (en excluant la séance actuelle)
                    List<Salle> disponibles = salleRepository.findSallesDisponiblesExcluding(
                        newDate, newHeureDebut, newHeureFin, id);
                    if (!disponibles.contains(salle)) {
                        return ResponseEntity.ok(Map.of(
                            "success", false,
                            "message", "Conflit: la salle " + salle.getCode() + " est déjà occupée sur ce créneau"
                        ));
                    }
                    seance.setSalle(salle);
                }
            } else {
                seance.setSalle(null);
            }
            
            if (payload.get("formateurId") != null && !payload.get("formateurId").toString().isEmpty()) {
                Long formateurId = Long.valueOf(payload.get("formateurId").toString());
                seance.setFormateur(formateurService.findById(formateurId));
            } else {
                seance.setFormateur(null);
            }
            
            seanceCoursService.update(id, seance);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Séance modifiée avec succès"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    /**
     * API: Supprimer une séance
     */
    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteSeanceApi(@PathVariable Long id) {
        try {
            seanceCoursService.deleteById(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Séance supprimée"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    /**
     * API: Récupérer les séances d'un groupe pour une semaine (JSON)
     */
    @GetMapping("/api/groupe/{groupeId}/semaine")
    @ResponseBody
    public ResponseEntity<?> getSeancesGroupeSemaine(
            @PathVariable Long groupeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate startOfWeek = date.with(WeekFields.of(Locale.FRANCE).dayOfWeek(), 1);
        LocalDate endOfWeek = startOfWeek.plusDays(5);
        
        List<SeanceCours> seances = seanceCoursService.findByGroupeAndDateBetween(groupeId, startOfWeek, endOfWeek);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "seances", convertSeancesToList(seances)
        ));
    }
    
    private List<Map<String, Object>> convertSeancesToList(List<SeanceCours> seances) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (SeanceCours s : seances) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("date", s.getDate().toString());
            map.put("heureDebut", s.getHeureDebut().getHour());
            map.put("heureFin", s.getHeureFin().getHour());
            if (s.getSalle() != null) {
                map.put("salleId", s.getSalle().getId());
                map.put("salle", s.getSalle().getCode());
            } else {
                map.put("salleId", null);
                map.put("salle", s.getSalleTexte() != null ? s.getSalleTexte() : "");
            }
            map.put("coursId", s.getCours() != null ? s.getCours().getId() : null);
            map.put("coursCode", s.getCours() != null ? s.getCours().getCode() : "");
            map.put("coursNom", s.getCours() != null ? s.getCours().getTitre() : "");
            map.put("formateurId", s.getFormateur() != null ? s.getFormateur().getId() : null);
            map.put("formateurNom", s.getFormateur() != null ? s.getFormateur().getNom() + " " + s.getFormateur().getPrenom() : "");
            list.add(map);
        }
        return list;
    }
    
    /**
     * API: Récupérer les salles disponibles pour un créneau
     */
    @GetMapping("/api/salles-disponibles")
    @ResponseBody
    public ResponseEntity<?> getSallesDisponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer heureDebut,
            @RequestParam Integer heureFin,
            @RequestParam(required = false) Long excludeSeanceId) {
        
        try {
            LocalTime debut = LocalTime.of(heureDebut, 0);
            LocalTime fin = LocalTime.of(heureFin, 0);
            
            List<Salle> salles;
            if (excludeSeanceId != null) {
                salles = salleRepository.findSallesDisponiblesExcluding(date, debut, fin, excludeSeanceId);
            } else {
                salles = salleRepository.findSallesDisponibles(date, debut, fin);
            }
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Salle s : salles) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getId());
                map.put("code", s.getCode());
                map.put("nom", s.getNom() != null ? s.getNom() : "");
                map.put("capacite", s.getCapacite());
                map.put("type", s.getType());
                map.put("display", s.getCode() + (s.getNom() != null ? " - " + s.getNom() : "") + 
                                   (s.getCapacite() != null ? " (" + s.getCapacite() + " places)" : ""));
                result.add(map);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "salles", result
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Erreur: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Récupérer toutes les salles actives
     */
    @GetMapping("/api/salles")
    @ResponseBody
    public ResponseEntity<?> getAllSalles() {
        List<Salle> salles = salleRepository.findByActifTrueOrderByCodeAsc();
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Salle s : salles) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("code", s.getCode());
            map.put("nom", s.getNom() != null ? s.getNom() : "");
            map.put("capacite", s.getCapacite());
            map.put("type", s.getType());
            result.add(map);
        }
        
        return ResponseEntity.ok(Map.of("success", true, "salles", result));
    }
}
