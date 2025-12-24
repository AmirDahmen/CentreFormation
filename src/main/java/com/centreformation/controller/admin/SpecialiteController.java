package com.centreformation.controller.admin;

import com.centreformation.entity.Specialite;
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

@Controller
@RequestMapping("/admin/specialites")
@RequiredArgsConstructor
public class SpecialiteController {

    private final SpecialiteService specialiteService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size,
                      @RequestParam(required = false) String keyword,
                      Model model) {
        
        Page<Specialite> specialitesPage;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("nom").ascending());
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            specialitesPage = specialiteService.search(keyword, pageRequest);
        } else {
            specialitesPage = specialiteService.findAll(pageRequest);
        }
        
        model.addAttribute("specialites", specialitesPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "specialites");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", specialitesPage.getTotalPages());
        
        return "admin/specialites/list";
    }

    @GetMapping("/nouvelle")
    public String showCreateForm(Model model) {
        model.addAttribute("specialite", new Specialite());
        model.addAttribute("activePage", "specialites");
        model.addAttribute("isEdit", false);
        return "admin/specialites/form";
    }

    @PostMapping("/nouvelle")
    public String create(@Valid @ModelAttribute Specialite specialite,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "specialites");
            return "admin/specialites/form";
        }
        
        try {
            specialiteService.save(specialite);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La spécialité " + specialite.getNom() + " a été créée avec succès.");
            return "redirect:/admin/specialites";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "specialites");
            return "admin/specialites/form";
        }
    }

    @GetMapping("/{id}/modifier")
    public String showEditForm(@PathVariable Long id, Model model) {
        Specialite specialite = specialiteService.findById(id);
        model.addAttribute("specialite", specialite);
        model.addAttribute("activePage", "specialites");
        model.addAttribute("isEdit", true);
        return "admin/specialites/form";
    }

    @PostMapping("/{id}/modifier")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute Specialite specialite,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "specialites");
            return "admin/specialites/form";
        }
        
        try {
            specialiteService.update(id, specialite);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La spécialité " + specialite.getNom() + " a été modifiée avec succès.");
            return "redirect:/admin/specialites";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "specialites");
            return "admin/specialites/form";
        }
    }

    @PostMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Specialite specialite = specialiteService.findById(id);
            specialiteService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La spécialité " + specialite.getNom() + " a été supprimée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression : " + e.getMessage());
        }
        return "redirect:/admin/specialites";
    }
}
