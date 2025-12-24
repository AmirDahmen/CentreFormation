package com.centreformation.controller.admin;

import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Groupe;
import com.centreformation.entity.Inscription;
import com.centreformation.entity.Note;
import com.centreformation.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/etudiants")
@RequiredArgsConstructor
public class EtudiantController {

    private final EtudiantService etudiantService;
    private final SpecialiteService specialiteService;
    private final GroupeService groupeService;
    private final InscriptionService inscriptionService;
    private final NoteService noteService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "10") int size,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long specialiteId,
                      Model model) {
        
        Page<Etudiant> etudiantsPage;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("nom").ascending());
        
        if (specialiteId != null) {
            etudiantsPage = etudiantService.findBySpecialite(specialiteId, pageRequest);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            etudiantsPage = etudiantService.search(keyword, pageRequest);
        } else {
            etudiantsPage = etudiantService.findAll(pageRequest);
        }
        
        model.addAttribute("etudiants", etudiantsPage);
        model.addAttribute("specialites", specialiteService.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("specialiteId", specialiteId);
        model.addAttribute("activePage", "etudiants");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", etudiantsPage.getTotalPages());
        
        return "admin/etudiants/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Etudiant etudiant = etudiantService.findById(id);
        List<Inscription> inscriptions = inscriptionService.findByEtudiant(id);
        List<Note> notes = noteService.findByEtudiant(id);
        Double moyenne = noteService.calculateAverageByEtudiant(id);
        
        model.addAttribute("etudiant", etudiant);
        model.addAttribute("inscriptions", inscriptions);
        model.addAttribute("notes", notes);
        model.addAttribute("moyenne", moyenne);
        model.addAttribute("activePage", "etudiants");
        
        return "admin/etudiants/detail";
    }

    @GetMapping("/nouveau")
    public String showCreateForm(Model model) {
        Etudiant etudiant = new Etudiant();
        etudiant.setDateInscription(LocalDate.now());
        
        model.addAttribute("etudiant", etudiant);
        model.addAttribute("specialites", specialiteService.findAll());
        model.addAttribute("groupes", groupeService.findAll());
        model.addAttribute("activePage", "etudiants");
        model.addAttribute("isEdit", false);
        
        return "admin/etudiants/form";
    }

    @PostMapping("/nouveau")
    public String create(@Valid @ModelAttribute Etudiant etudiant,
                        BindingResult result,
                        @RequestParam(required = false) List<Long> groupeIds,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("groupes", groupeService.findAll());
            model.addAttribute("selectedGroupes", groupeIds != null ? new HashSet<>(groupeIds) : new HashSet<Long>());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "etudiants");
            return "admin/etudiants/form";
        }
        
        try {
            Etudiant savedEtudiant = etudiantService.save(etudiant);
            
            // Associer les groupes
            if (groupeIds != null && !groupeIds.isEmpty()) {
                etudiantService.setGroupes(savedEtudiant.getId(), new HashSet<>(groupeIds));
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "L'étudiant " + etudiant.getPrenom() + " " + etudiant.getNom() + " a été créé avec succès.");
            return "redirect:/admin/etudiants/" + savedEtudiant.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("groupes", groupeService.findAll());
            model.addAttribute("selectedGroupes", groupeIds != null ? new HashSet<>(groupeIds) : new HashSet<Long>());
            model.addAttribute("isEdit", false);
            model.addAttribute("activePage", "etudiants");
            return "admin/etudiants/form";
        }
    }

    @GetMapping("/{id}/modifier")
    public String showEditForm(@PathVariable Long id, Model model) {
        Etudiant etudiant = etudiantService.findById(id);
        
        model.addAttribute("etudiant", etudiant);
        model.addAttribute("specialites", specialiteService.findAll());
        model.addAttribute("groupes", groupeService.findAll());
        model.addAttribute("selectedGroupes", etudiant.getGroupes().stream()
                .map(Groupe::getId).collect(Collectors.toSet()));
        model.addAttribute("activePage", "etudiants");
        model.addAttribute("isEdit", true);
        
        return "admin/etudiants/form";
    }

    @PostMapping("/{id}/modifier")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute Etudiant etudiant,
                        BindingResult result,
                        @RequestParam(required = false) List<Long> groupeIds,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("groupes", groupeService.findAll());
            model.addAttribute("selectedGroupes", groupeIds != null ? new HashSet<>(groupeIds) : new HashSet<Long>());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "etudiants");
            return "admin/etudiants/form";
        }
        
        try {
            etudiantService.update(id, etudiant);
            
            // Mettre à jour les groupes
            Set<Long> groupeIdSet = groupeIds != null ? new HashSet<>(groupeIds) : new HashSet<>();
            etudiantService.setGroupes(id, groupeIdSet);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "L'étudiant " + etudiant.getPrenom() + " " + etudiant.getNom() + " a été modifié avec succès.");
            return "redirect:/admin/etudiants/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("specialites", specialiteService.findAll());
            model.addAttribute("groupes", groupeService.findAll());
            model.addAttribute("selectedGroupes", groupeIds != null ? new HashSet<>(groupeIds) : new HashSet<Long>());
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", "etudiants");
            return "admin/etudiants/form";
        }
    }

    @PostMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Etudiant etudiant = etudiantService.findById(id);
            etudiantService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "L'étudiant " + etudiant.getPrenom() + " " + etudiant.getNom() + " a été supprimé avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression : " + e.getMessage());
        }
        return "redirect:/admin/etudiants";
    }
}
