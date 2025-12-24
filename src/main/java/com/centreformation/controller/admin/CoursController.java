package com.centreformation.controller.admin;

import com.centreformation.entity.Cours;
import com.centreformation.service.CoursService;
import com.centreformation.service.FormateurService;
import com.centreformation.service.GroupeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/cours")
@RequiredArgsConstructor
public class CoursController {

    private final CoursService coursService;
    private final FormateurService formateurService;
    private final GroupeService groupeService;
    
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Exclure formateur et groupes du binding automatique
        // pour éviter les problèmes de clé étrangère
        binder.setDisallowedFields("formateur", "groupes");
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size,
                      @RequestParam(required = false) String keyword,
                      Model model) {
        
        Page<Cours> coursPage;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("code").ascending());
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            coursPage = coursService.search(keyword, pageRequest);
        } else {
            coursPage = coursService.findAll(pageRequest);
        }
        
        model.addAttribute("cours", coursPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "cours");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", coursPage.getTotalPages());
        
        return "admin/cours/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Cours cours = coursService.findById(id);
        model.addAttribute("cours", cours);
        model.addAttribute("activePage", "cours");
        return "admin/cours/detail";
    }

    @GetMapping("/nouveau")
    public String showCreateForm(Model model) {
        model.addAttribute("cours", new Cours());
        model.addAttribute("formateurs", formateurService.findAll());
        model.addAttribute("groupes", groupeService.findAll());
        model.addAttribute("activePage", "cours");
        model.addAttribute("isEdit", false);
        return "admin/cours/form";
    }

    @PostMapping("/nouveau")
    public String create(@Valid @ModelAttribute Cours cours,
                        BindingResult result,
                        @RequestParam(required = false) Long formateurId,
                        @RequestParam(required = false) List<Long> groupeIds,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        System.out.println("DEBUG - formateurId reçu: " + formateurId);
        System.out.println("DEBUG - Cours formateur avant: " + (cours.getFormateur() != null ? cours.getFormateur().getId() : "null"));
        
        if (result.hasErrors()) {
            model.addAttribute("formateurs", formateurService.findAll());
            model.addAttribute("groupes", groupeService.findAll());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "cours");
            return "admin/cours/form";
        }
        
        try {
            // Définir le formateur si fourni
            cours.setFormateur(null); // Reset d'abord
            if (formateurId != null && formateurId > 0) {
                try {
                    cours.setFormateur(formateurService.findById(formateurId));
                } catch (Exception e) {
                    model.addAttribute("errorMessage", "Formateur introuvable avec l'ID: " + formateurId);
                    model.addAttribute("formateurs", formateurService.findAll());
                    model.addAttribute("groupes", groupeService.findAll());
                    model.addAttribute("isEdit", false);
                    model.addAttribute("activePage", "cours");
                    return "admin/cours/form";
                }
            }
            
            Cours saved = coursService.save(cours);
            
            if (groupeIds != null && !groupeIds.isEmpty()) {
                coursService.setGroupes(saved.getId(), new HashSet<>(groupeIds));
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Le cours " + cours.getTitre() + " a été créé avec succès.");
            return "redirect:/admin/cours/" + saved.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("formateurs", formateurService.findAll());
            model.addAttribute("groupes", groupeService.findAll());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "cours");
            return "admin/cours/form";
        }
    }

    @GetMapping("/{id}/modifier")
    public String showEditForm(@PathVariable Long id, Model model) {
        Cours cours = coursService.findById(id);
        model.addAttribute("cours", cours);
        model.addAttribute("formateurs", formateurService.findAll());
        model.addAttribute("groupes", groupeService.findAll());
        model.addAttribute("selectedGroupes", cours.getGroupes().stream()
                .map(g -> g.getId()).toList());
        model.addAttribute("activePage", "cours");
        model.addAttribute("isEdit", true);
        return "admin/cours/form";
    }

    @PostMapping("/{id}/modifier")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute Cours cours,
                        BindingResult result,
                        @RequestParam(required = false) Long formateurId,
                        @RequestParam(required = false) List<Long> groupeIds,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("formateurs", formateurService.findAll());
            model.addAttribute("groupes", groupeService.findAll());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "cours");
            return "admin/cours/form";
        }
        
        try {
            // Définir le formateur si fourni
            cours.setFormateur(null); // Reset d'abord
            if (formateurId != null && formateurId > 0) {
                try {
                    cours.setFormateur(formateurService.findById(formateurId));
                } catch (Exception e) {
                    model.addAttribute("errorMessage", "Formateur introuvable avec l'ID: " + formateurId);
                    model.addAttribute("formateurs", formateurService.findAll());
                    model.addAttribute("groupes", groupeService.findAll());
                    model.addAttribute("isEdit", true);
                    model.addAttribute("activePage", "cours");
                    return "admin/cours/form";
                }
            }
            
            coursService.update(id, cours);
            
            Set<Long> groupeIdSet = groupeIds != null ? new HashSet<>(groupeIds) : new HashSet<>();
            coursService.setGroupes(id, groupeIdSet);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Le cours " + cours.getTitre() + " a été modifié avec succès.");
            return "redirect:/admin/cours/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("formateurs", formateurService.findAll());
            model.addAttribute("groupes", groupeService.findAll());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "cours");
            return "admin/cours/form";
        }
    }

    @PostMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Cours cours = coursService.findById(id);
            coursService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Le cours " + cours.getTitre() + " a été supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression : " + e.getMessage());
        }
        return "redirect:/admin/cours";
    }
}
