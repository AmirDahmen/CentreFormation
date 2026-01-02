package com.centreformation.controller.admin;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Inscription;
import com.centreformation.entity.Note;
import com.centreformation.service.CoursService;
import com.centreformation.service.EtudiantService;
import com.centreformation.service.InscriptionService;
import com.centreformation.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final EtudiantService etudiantService;
    private final CoursService coursService;
    private final InscriptionService inscriptionService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "15") int size,
                      @RequestParam(required = false) Long etudiantId,
                      @RequestParam(required = false) Long coursId,
                      Model model) {
        
        PageRequest pageRequest = PageRequest.of(page, size, 
            Sort.by("dateSaisie").descending());
        
        Page<Note> notesPage;
        if (etudiantId != null) {
            notesPage = noteService.findByEtudiantId(etudiantId, pageRequest);
        } else if (coursId != null) {
            notesPage = noteService.findByCoursId(coursId, pageRequest);
        } else {
            notesPage = noteService.findAll(pageRequest);
        }
        
        model.addAttribute("notes", notesPage);
        model.addAttribute("etudiants", etudiantService.findAll());
        model.addAttribute("cours", coursService.findAll());
        model.addAttribute("allCours", coursService.findAll());
        model.addAttribute("etudiantId", etudiantId);
        model.addAttribute("coursId", coursId);
        model.addAttribute("activePage", "notes");
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notesPage.getTotalPages());
        
        return "admin/notes/list";
    }

    @GetMapping("/saisir-par-cours")
    public String saisirParCours(@RequestParam(required = false) Long coursId, Model model) {
        model.addAttribute("allCours", coursService.findAll());
        model.addAttribute("coursId", coursId);
        model.addAttribute("activePage", "notes");

        if (coursId != null) {
            List<Inscription> inscriptions = inscriptionService.findByCours(coursId);
            List<Note> notes = noteService.findByCours(coursId);
            Map<Long, Double> notesMap = notes.stream()
                .collect(Collectors.toMap(note -> note.getEtudiant().getId(), Note::getValeur, (a, b) -> a));

            model.addAttribute("inscriptions", inscriptions);
            model.addAttribute("notesMap", notesMap);
        }

        return "admin/notes/form";
    }

    @PostMapping("/saisir-par-cours")
    public String saisirParCoursPost(@RequestParam Long coursId,
                                     @RequestParam(required = false) List<Long> etudiantIds,
                                     @RequestParam(name = "notes", required = false) List<String> notesValues,
                                     @RequestParam(name = "observations", required = false) List<String> observations,
                                     RedirectAttributes redirectAttributes) {
        if (etudiantIds == null || etudiantIds.isEmpty() || notesValues == null || notesValues.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Aucune note a enregistrer.");
            return "redirect:/admin/notes/saisir-par-cours?coursId=" + coursId;
        }

        int saved = 0;
        int count = Math.min(etudiantIds.size(), notesValues.size());
        try {
            for (int i = 0; i < count; i++) {
                String raw = notesValues.get(i);
                if (raw == null || raw.trim().isEmpty()) {
                    continue;
                }
                Double valeur = Double.parseDouble(raw.trim().replace(",", "."));
                String commentaire = (observations != null && observations.size() > i) ? observations.get(i) : null;
                noteService.saisirNote(etudiantIds.get(i), coursId, valeur, commentaire, null);
                saved++;
            }
            if (saved > 0) {
                redirectAttributes.addFlashAttribute("successMessage", saved + " note(s) enregistree(s).");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Aucune note a enregistrer.");
            }
            return "redirect:/admin/notes/saisir-par-cours?coursId=" + coursId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
            return "redirect:/admin/notes/saisir-par-cours?coursId=" + coursId;
        }
    }

    @GetMapping("/cours/{coursId}/saisir")
    public String saisirNotesPourCours(@PathVariable Long coursId, Model model) {
        Cours cours = coursService.findById(coursId);
        List<Inscription> inscriptions = inscriptionService.findByCours(coursId);
        List<Note> notes = noteService.findByCours(coursId);
        
        model.addAttribute("cours", cours);
        model.addAttribute("inscriptions", inscriptions);
        model.addAttribute("notes", notes);
        model.addAttribute("activePage", "notes");
        
        return "admin/notes/saisir-cours";
    }

    @PostMapping("/saisir")
    public String saisirNote(@RequestParam Long etudiantId,
                            @RequestParam Long coursId,
                            @RequestParam Double valeur,
                            @RequestParam(required = false) String commentaire,
                            RedirectAttributes redirectAttributes) {
        try {
            noteService.saisirNote(etudiantId, coursId, valeur, commentaire, null);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La note a été enregistrée avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur : " + e.getMessage());
        }
        return "redirect:/admin/notes/cours/" + coursId + "/saisir";
    }

    @PostMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id,
                          @RequestParam Double valeur,
                          @RequestParam(required = false) String commentaire,
                          RedirectAttributes redirectAttributes) {
        try {
            Note note = noteService.findById(id);
            note.setValeur(valeur);
            note.setCommentaire(commentaire);
            noteService.update(id, note);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La note a été modifiée avec succès.");
            return "redirect:/admin/notes/cours/" + note.getCours().getId() + "/saisir";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur : " + e.getMessage());
            return "redirect:/admin/notes";
        }
    }

    @PostMapping("/{id}/supprimer")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Note note = noteService.findById(id);
            Long coursId = note.getCours().getId();
            noteService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "La note a été supprimée avec succès.");
            return "redirect:/admin/notes/cours/" + coursId + "/saisir";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erreur lors de la suppression : " + e.getMessage());
            return "redirect:/admin/notes";
        }
    }
}
