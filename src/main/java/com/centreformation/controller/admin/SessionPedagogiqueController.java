package com.centreformation.controller.admin;

import com.centreformation.entity.SessionPedagogique;
import com.centreformation.service.SessionPedagogiqueService;
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
@RequestMapping("/admin/sessions")
@RequiredArgsConstructor
public class SessionPedagogiqueController {

    private final SessionPedagogiqueService sessionService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size,
                      Model model) {
        
        PageRequest pageRequest = PageRequest.of(page, size, 
            Sort.by("anneeScolaire").descending());
        Page<SessionPedagogique> sessionsPage = sessionService.findAll(pageRequest);
        
        model.addAttribute("sessions", sessionsPage);
        model.addAttribute("activePage", "sessions");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", sessionsPage.getTotalPages());
        
        return "admin/sessions/list";
    }

    @GetMapping("/nouvelle")
    public String showCreateForm(Model model) {
        model.addAttribute("sessionPedagogique", new SessionPedagogique());
        model.addAttribute("activePage", "sessions");
        model.addAttribute("isEdit", false);
        return "admin/sessions/form";
    }

    @PostMapping("/nouvelle")
    public String create(@Valid @ModelAttribute("sessionPedagogique") SessionPedagogique sessionPedagogique,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "sessions");
            return "admin/sessions/form";
        }
        
        try {
            sessionService.save(sessionPedagogique);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La session " + sessionPedagogique.getAnneeScolaire() + " a été créée avec succès.");
            return "redirect:/admin/sessions";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "sessions");
            return "admin/sessions/form";
        }
    }

    @GetMapping("/{id}/modifier")
    public String showEditForm(@PathVariable Long id, Model model) {
        SessionPedagogique sessionPedagogique = sessionService.findById(id);
        model.addAttribute("sessionPedagogique", sessionPedagogique);
        model.addAttribute("activePage", "sessions");
        model.addAttribute("isEdit", true);
        return "admin/sessions/form";
    }

    @PostMapping("/{id}/modifier")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("sessionPedagogique") SessionPedagogique sessionPedagogique,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "sessions");
            return "admin/sessions/form";
        }
        
        try {
            sessionService.update(id, sessionPedagogique);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La session " + sessionPedagogique.getAnneeScolaire() + " a été modifiée avec succès.");
            return "redirect:/admin/sessions";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "sessions");
            return "admin/sessions/form";
        }
    }

    @PostMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            SessionPedagogique session = sessionService.findById(id);
            sessionService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La session " + session.getAnneeScolaire() + " a été supprimée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression : " + e.getMessage());
        }
        return "redirect:/admin/sessions";
    }
}
