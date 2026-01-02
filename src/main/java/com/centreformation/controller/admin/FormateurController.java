package com.centreformation.controller.admin;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Formateur;
import com.centreformation.service.FormateurService;
import com.centreformation.service.SpecialiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/formateurs")
@RequiredArgsConstructor
public class FormateurController {

    private final FormateurService formateurService;
    private final SpecialiteService specialiteService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size,
                      @RequestParam(required = false) String keyword,
                      Model model) {
        
        Page<Formateur> formateursPage;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("nom").ascending());
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            formateursPage = formateurService.search(keyword, pageRequest);
        } else {
            formateursPage = formateurService.findAll(pageRequest);
        }
        
        model.addAttribute("formateurs", formateursPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "formateurs");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", formateursPage.getTotalPages());
        
        return "admin/formateurs/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Formateur formateur = formateurService.findById(id);
        List<Cours> cours = formateurService.findCoursByFormateur(id);
        
        model.addAttribute("formateur", formateur);
        model.addAttribute("cours", cours);
        model.addAttribute("activePage", "formateurs");
        
        return "admin/formateurs/detail";
    }

    @GetMapping("/nouveau")
    public String showCreateForm(Model model) {
        model.addAttribute("formateur", new Formateur());
        model.addAttribute("specialites", specialiteService.findAll());
        model.addAttribute("activePage", "formateurs");
        model.addAttribute("isEdit", false);
        return "admin/formateurs/form";
    }

    @PostMapping("/nouveau")
    public String create(@Valid @ModelAttribute Formateur formateur,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "formateurs");
            return "admin/formateurs/form";
        }
        
        try {
            Formateur saved = formateurService.save(formateur);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Le formateur " + formateur.getPrenom() + " " + formateur.getNom() + " a été créé avec succès.");
            return "redirect:/admin/formateurs/" + saved.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "formateurs");
            return "admin/formateurs/form";
        }
    }

    @GetMapping("/{id}/modifier")
    public String showEditForm(@PathVariable Long id, Model model) {
        Formateur formateur = formateurService.findById(id);
        model.addAttribute("formateur", formateur);
        model.addAttribute("specialites", specialiteService.findAll());
        model.addAttribute("activePage", "formateurs");
        model.addAttribute("isEdit", true);
        return "admin/formateurs/form";
    }

    @PostMapping("/{id}/modifier")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute Formateur formateur,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "formateurs");
            return "admin/formateurs/form";
        }
        
        try {
            formateurService.update(id, formateur);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Le formateur " + formateur.getPrenom() + " " + formateur.getNom() + " a été modifié avec succès.");
            return "redirect:/admin/formateurs/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "formateurs");
            return "admin/formateurs/form";
        }
    }

    @PostMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Formateur formateur = formateurService.findById(id);
            formateurService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Le formateur " + formateur.getPrenom() + " " + formateur.getNom() + " a été supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression : " + e.getMessage());
        }
        return "redirect:/admin/formateurs";
    }
}
