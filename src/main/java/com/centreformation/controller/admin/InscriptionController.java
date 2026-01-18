package com.centreformation.controller.admin;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Groupe;
import com.centreformation.entity.Inscription;
import com.centreformation.service.CoursService;
import com.centreformation.service.EtudiantService;
import com.centreformation.service.GroupeService;
import com.centreformation.service.InscriptionService;
import com.centreformation.service.SessionPedagogiqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/admin/inscriptions")
@RequiredArgsConstructor
public class InscriptionController {

    private final InscriptionService inscriptionService;
    private final EtudiantService etudiantService;
    private final CoursService coursService;
    private final GroupeService groupeService;
    private final SessionPedagogiqueService sessionService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "15") int size,
                      @RequestParam(required = false) Long etudiantId,
                      @RequestParam(required = false) Long coursId,
                      @RequestParam(required = false) Inscription.StatutInscription statut,
                      Model model) {
        
        PageRequest pageRequest = PageRequest.of(page, size, 
            Sort.by("dateInscription").descending());
        
        Page<Inscription> inscriptionsPage = inscriptionService.search(
            etudiantId, coursId, statut, pageRequest);
        
        // Compter les inscriptions en attente pour l'alerte
        long countEnAttente = inscriptionService.countByStatut(Inscription.StatutInscription.EN_ATTENTE);
        
        model.addAttribute("inscriptions", inscriptionsPage);
        model.addAttribute("etudiants", etudiantService.findAll());
        model.addAttribute("allCours", coursService.findAll());
        model.addAttribute("statuts", Inscription.StatutInscription.values());
        model.addAttribute("etudiantId", etudiantId);
        model.addAttribute("coursId", coursId);
        model.addAttribute("statut", statut);
        model.addAttribute("activePage", "inscriptions");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", inscriptionsPage.getTotalPages());
        model.addAttribute("countEnAttente", countEnAttente);
        
        return "admin/inscriptions/list";
    }

    @GetMapping("/nouvelle")
    public String showCreateForm(Model model) {
        model.addAttribute("etudiants", etudiantService.findAll());
        model.addAttribute("cours", coursService.findAll());
        model.addAttribute("activePage", "inscriptions");
        return "admin/inscriptions/form";
    }

    @PostMapping("/nouvelle")
    public String create(@RequestParam Long etudiantId,
                        @RequestParam Long coursId,
                        RedirectAttributes redirectAttributes) {
        try {
            inscriptionService.inscrire(etudiantId, coursId);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La demande d'inscription a été créée avec succès (en attente de validation).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/inscriptions";
    }

    /**
     * Affiche le formulaire de validation d'une inscription
     */
    @GetMapping("/{id}/valider")
    public String showValiderForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Inscription inscription = inscriptionService.findById(id);
            
            if (inscription.getStatut() != Inscription.StatutInscription.EN_ATTENTE) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Seules les inscriptions en attente peuvent être validées.");
                return "redirect:/admin/inscriptions";
            }
            
            model.addAttribute("inscription", inscription);
            
            // Récupérer les groupes du cours (relation OneToMany maintenant)
            Cours cours = inscription.getCours();
            Set<Groupe> groupesDuCours = cours.getGroupes();
            model.addAttribute("groupesDuCours", groupesDuCours);
            
            // Si aucun groupe n'existe pour ce cours
            if (groupesDuCours == null || groupesDuCours.isEmpty()) {
                model.addAttribute("aucunGroupeAssigne", true);
            }
            
            model.addAttribute("activePage", "inscriptions");
            
            return "admin/inscriptions/valider";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Inscription non trouvée : " + e.getMessage());
            return "redirect:/admin/inscriptions";
        }
    }

    /**
     * Valide une inscription et assigne un groupe
     */
    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id,
                         @RequestParam Long groupeId,
                         RedirectAttributes redirectAttributes) {
        try {
            inscriptionService.valider(id, groupeId);
            redirectAttributes.addFlashAttribute("successMessage", 
                "L'inscription a été validée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la validation : " + e.getMessage());
        }
        return "redirect:/admin/inscriptions";
    }

    /**
     * Refuse une inscription
     */
    @PostMapping("/{id}/refuser")
    public String refuser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            inscriptionService.refuser(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "L'inscription a été refusée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors du refus : " + e.getMessage());
        }
        return "redirect:/admin/inscriptions";
    }

    @GetMapping("/{id}/annuler")
    public String annulerGet(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return annuler(id, redirectAttributes);
    }

    @PostMapping("/{id}/annuler")
    public String annuler(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            inscriptionService.annuler(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "L'inscription a été annulée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/inscriptions";
    }

    @GetMapping("/{id}/supprimer")
    public String deleteGet(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return delete(id, redirectAttributes);
    }

    @PostMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            inscriptionService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "L'inscription a été supprimée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression : " + e.getMessage());
        }
        return "redirect:/admin/inscriptions";
    }
}
