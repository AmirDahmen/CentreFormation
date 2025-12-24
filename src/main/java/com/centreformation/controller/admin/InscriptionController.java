package com.centreformation.controller.admin;

import com.centreformation.entity.Inscription;
import com.centreformation.service.CoursService;
import com.centreformation.service.EtudiantService;
import com.centreformation.service.InscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/inscriptions")
@RequiredArgsConstructor
public class InscriptionController {

    private final InscriptionService inscriptionService;
    private final EtudiantService etudiantService;
    private final CoursService coursService;

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
        
        model.addAttribute("inscriptions", inscriptionsPage);
        model.addAttribute("etudiants", etudiantService.findAll());
        model.addAttribute("cours", coursService.findAll());
        model.addAttribute("statuts", Inscription.StatutInscription.values());
        model.addAttribute("etudiantId", etudiantId);
        model.addAttribute("coursId", coursId);
        model.addAttribute("statut", statut);
        model.addAttribute("activePage", "inscriptions");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", inscriptionsPage.getTotalPages());
        
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
                "L'inscription a été créée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/inscriptions";
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
