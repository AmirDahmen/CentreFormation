package com.centreformation.controller.admin;

import com.centreformation.entity.Salle;
import com.centreformation.repository.SalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/salles")
@RequiredArgsConstructor
public class SalleController {

    private final SalleRepository salleRepository;

    @GetMapping
    public String list(Model model) {
        List<Salle> salles = salleRepository.findAll();
        model.addAttribute("salles", salles);
        
        // Calcul des statistiques
        long totalActives = salles.stream().filter(s -> Boolean.TRUE.equals(s.getActif())).count();
        long totalLabosInfo = salles.stream().filter(s -> "INFORMATIQUE".equals(s.getType())).count();
        int capaciteTotale = salles.stream()
                .mapToInt(s -> s.getCapacite() != null ? s.getCapacite() : 0)
                .sum();
        
        model.addAttribute("totalActives", totalActives);
        model.addAttribute("totalLabosInfo", totalLabosInfo);
        model.addAttribute("capaciteTotale", capaciteTotale);
        model.addAttribute("activePage", "salles");
        return "admin/salles/list";
    }

    @PostMapping("/nouvelle")
    public String create(@ModelAttribute Salle salle, RedirectAttributes redirectAttributes) {
        try {
            // Vérifier unicité du code
            if (salleRepository.findByCode(salle.getCode()).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Une salle avec ce code existe déjà");
                return "redirect:/admin/salles";
            }
            
            if (salle.getActif() == null) {
                salle.setActif(true);
            }
            salleRepository.save(salle);
            redirectAttributes.addFlashAttribute("success", "Salle créée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création: " + e.getMessage());
        }
        return "redirect:/admin/salles";
    }

    @PostMapping("/{id}/modifier")
    public String update(@PathVariable Long id, @ModelAttribute Salle salle, RedirectAttributes redirectAttributes) {
        try {
            Salle existing = salleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Salle non trouvée"));
            
            // Vérifier unicité du code si modifié
            if (!existing.getCode().equals(salle.getCode())) {
                if (salleRepository.findByCode(salle.getCode()).isPresent()) {
                    redirectAttributes.addFlashAttribute("error", "Une salle avec ce code existe déjà");
                    return "redirect:/admin/salles";
                }
            }
            
            existing.setCode(salle.getCode());
            existing.setNom(salle.getNom());
            existing.setCapacite(salle.getCapacite());
            existing.setType(salle.getType());
            existing.setBatiment(salle.getBatiment());
            existing.setEtage(salle.getEtage());
            existing.setActif(salle.getActif() != null ? salle.getActif() : true);
            existing.setHasProjecteur(salle.getHasProjecteur() != null ? salle.getHasProjecteur() : false);
            existing.setHasOrdinateurs(salle.getHasOrdinateurs() != null ? salle.getHasOrdinateurs() : false);
            existing.setHasTableauBlanc(salle.getHasTableauBlanc() != null ? salle.getHasTableauBlanc() : true);
            
            salleRepository.save(existing);
            redirectAttributes.addFlashAttribute("success", "Salle modifiée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la modification: " + e.getMessage());
        }
        return "redirect:/admin/salles";
    }

    @GetMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Salle salle = salleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Salle non trouvée"));
            
            // Désactiver plutôt que supprimer pour garder l'historique des séances
            salle.setActif(false);
            salleRepository.save(salle);
            
            redirectAttributes.addFlashAttribute("success", "Salle désactivée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/salles";
    }
    
    @GetMapping("/{id}/activer")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Salle salle = salleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Salle non trouvée"));
            
            salle.setActif(true);
            salleRepository.save(salle);
            
            redirectAttributes.addFlashAttribute("success", "Salle réactivée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/salles";
    }
}
