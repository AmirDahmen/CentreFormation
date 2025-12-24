package com.centreformation.controller.admin;

import com.centreformation.entity.Groupe;
import com.centreformation.service.GroupeService;
import com.centreformation.service.SessionPedagogiqueService;
import com.centreformation.service.SpecialiteService;
import com.centreformation.service.EtudiantService;
import com.centreformation.service.CoursService;
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

import java.util.HashSet;
import java.util.List;

@Controller
@RequestMapping("/admin/groupes")
@RequiredArgsConstructor
public class GroupeController {

    private final GroupeService groupeService;
    private final SessionPedagogiqueService sessionService;
    private final SpecialiteService specialiteService;
    private final EtudiantService etudiantService;
    private final CoursService coursService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size,
                      @RequestParam(required = false) String keyword,
                      Model model) {
        
        Page<Groupe> groupesPage;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("nom").ascending());
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            groupesPage = groupeService.search(keyword, pageRequest);
        } else {
            groupesPage = groupeService.findAll(pageRequest);
        }
        
        model.addAttribute("groupes", groupesPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", "groupes");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", groupesPage.getTotalPages());
        
        return "admin/groupes/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Groupe groupe = groupeService.findById(id);
        model.addAttribute("groupe", groupe);
        model.addAttribute("activePage", "groupes");
        return "admin/groupes/detail";
    }

    @GetMapping("/nouveau")
    public String showCreateForm(Model model) {
        model.addAttribute("groupe", new Groupe());
        model.addAttribute("sessions", sessionService.findAll());
        model.addAttribute("specialites", specialiteService.findAll());
        model.addAttribute("etudiants", etudiantService.findAll());
        model.addAttribute("cours", coursService.findAll());
        model.addAttribute("activePage", "groupes");
        model.addAttribute("isEdit", false);
        return "admin/groupes/form";
    }

    @PostMapping("/nouveau")
    public String create(@Valid @ModelAttribute Groupe groupe,
                        BindingResult result,
                        @RequestParam(required = false) List<Long> etudiantIds,
                        @RequestParam(required = false) List<Long> coursIds,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("sessions", sessionService.findAll());
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("etudiants", etudiantService.findAll());
            model.addAttribute("cours", coursService.findAll());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "groupes");
            return "admin/groupes/form";
        }
        
        try {
            Groupe saved = groupeService.save(groupe);
            
            if (etudiantIds != null && !etudiantIds.isEmpty()) {
                groupeService.setEtudiants(saved.getId(), new HashSet<>(etudiantIds));
            }
            if (coursIds != null && !coursIds.isEmpty()) {
                groupeService.setCours(saved.getId(), new HashSet<>(coursIds));
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Le groupe " + groupe.getNom() + " a été créé avec succès.");
            return "redirect:/admin/groupes/" + saved.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("sessions", sessionService.findAll());
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("etudiants", etudiantService.findAll());
            model.addAttribute("cours", coursService.findAll());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "groupes");
            return "admin/groupes/form";
        }
    }

    @GetMapping("/{id}/modifier")
    public String showEditForm(@PathVariable Long id, Model model) {
        Groupe groupe = groupeService.findById(id);
        model.addAttribute("groupe", groupe);
        model.addAttribute("sessions", sessionService.findAll());
        model.addAttribute("specialites", specialiteService.findAll());
        model.addAttribute("etudiants", etudiantService.findAll());
        model.addAttribute("cours", coursService.findAll());
        model.addAttribute("selectedEtudiants", groupe.getEtudiants().stream()
                .map(e -> e.getId()).toList());
        model.addAttribute("selectedCours", groupe.getCours().stream()
                .map(c -> c.getId()).toList());
        model.addAttribute("activePage", "groupes");
        model.addAttribute("isEdit", true);
        return "admin/groupes/form";
    }

    @PostMapping("/{id}/modifier")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute Groupe groupe,
                        BindingResult result,
                        @RequestParam(required = false) List<Long> etudiantIds,
                        @RequestParam(required = false) List<Long> coursIds,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("sessions", sessionService.findAll());
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("etudiants", etudiantService.findAll());
            model.addAttribute("cours", coursService.findAll());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "groupes");
            return "admin/groupes/form";
        }
        
        try {
            groupeService.update(id, groupe);
            
            groupeService.setEtudiants(id, etudiantIds != null ? new HashSet<>(etudiantIds) : new HashSet<>());
            groupeService.setCours(id, coursIds != null ? new HashSet<>(coursIds) : new HashSet<>());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Le groupe " + groupe.getNom() + " a été modifié avec succès.");
            return "redirect:/admin/groupes/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("sessions", sessionService.findAll());
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("etudiants", etudiantService.findAll());
            model.addAttribute("cours", coursService.findAll());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "groupes");
            return "admin/groupes/form";
        }
    }

    @PostMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Groupe groupe = groupeService.findById(id);
            groupeService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Le groupe " + groupe.getNom() + " a été supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression : " + e.getMessage());
        }
        return "redirect:/admin/groupes";
    }
}
