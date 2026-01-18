package com.centreformation.controller.admin;

import com.centreformation.entity.Inscription;
import com.centreformation.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final EtudiantService etudiantService;
    private final FormateurService formateurService;
    private final CoursService coursService;
    private final InscriptionService inscriptionService;
    private final NoteService noteService;
    private final GroupeService groupeService;
    private final SpecialiteService specialiteService;
    private final SessionPedagogiqueService sessionService;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        // Statistiques
        model.addAttribute("totalEtudiants", etudiantService.count());
        model.addAttribute("totalFormateurs", formateurService.count());
        model.addAttribute("totalCours", coursService.count());
        model.addAttribute("totalInscriptions", inscriptionService.count());
        model.addAttribute("totalNotes", noteService.count());
        model.addAttribute("totalGroupes", groupeService.count());
        model.addAttribute("totalSpecialites", specialiteService.count());
        model.addAttribute("totalSessions", sessionService.count());
        
        // Inscriptions en attente de validation
        model.addAttribute("inscriptionsEnAttente", 
            inscriptionService.countByStatut(Inscription.StatutInscription.EN_ATTENTE));
        
        model.addAttribute("activePage", "dashboard");
        
        return "admin/dashboard";
    }
}
