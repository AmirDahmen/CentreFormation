package com.centreformation.service.impl;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Note;
import com.centreformation.repository.CoursRepository;
import com.centreformation.repository.EtudiantRepository;
import com.centreformation.repository.FormateurRepository;
import com.centreformation.repository.NoteRepository;
import com.centreformation.service.EmailService;
import com.centreformation.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final EtudiantRepository etudiantRepository;
    private final CoursRepository coursRepository;
    private final FormateurRepository formateurRepository;
    private final EmailService emailService;

    @Override
    public List<Note> findAll() {
        return noteRepository.findAll();
    }

    @Override
    public Page<Note> findAll(Pageable pageable) {
        return noteRepository.findAll(pageable);
    }

    @Override
    public Note findById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note non trouvée avec l'ID: " + id));
    }

    @Override
    public List<Note> findByEtudiant(Long etudiantId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        return noteRepository.findByEtudiant(etudiant);
    }

    @Override
    public List<Note> findByCours(Long coursId) {
        // Utiliser la requête avec fetch pour éviter lazy loading
        return noteRepository.findByCoursIdWithDetails(coursId);
    }

    @Override
    public Page<Note> findByEtudiantId(Long etudiantId, Pageable pageable) {
        return noteRepository.findByEtudiantId(etudiantId, pageable);
    }

    @Override
    public Page<Note> findByCoursId(Long coursId, Pageable pageable) {
        return noteRepository.findByCoursId(coursId, pageable);
    }

    @Override
    public Note save(Note note) {
        // Validation de la valeur de la note
        if (note.getValeur() < 0 || note.getValeur() > 20) {
            throw new RuntimeException("La note doit être comprise entre 0 et 20");
        }
        
        // Vérifier si une note existe déjà pour cet étudiant et ce cours
        if (note.getId() == null && 
            noteRepository.existsByEtudiantAndCours(note.getEtudiant(), note.getCours())) {
            throw new RuntimeException("Une note existe déjà pour cet étudiant dans ce cours");
        }
        
        if (note.getDateSaisie() == null) {
            note.setDateSaisie(LocalDateTime.now());
        }
        
        return noteRepository.save(note);
    }

    @Override
    public Note update(Long id, Note note) {
        Note existing = findById(id);
        
        // Validation de la valeur de la note
        if (note.getValeur() < 0 || note.getValeur() > 20) {
            throw new RuntimeException("La note doit être comprise entre 0 et 20");
        }
        
        existing.setValeur(note.getValeur());
        existing.setCommentaire(note.getCommentaire());
        existing.setDateSaisie(LocalDateTime.now());
        existing.setFormateur(note.getFormateur());
        
        return noteRepository.save(existing);
    }

    @Override
    public Note updateSimple(Long id, Double valeur, String commentaire) {
        Note existing = findById(id);
        
        // Validation de la valeur de la note
        if (valeur < 0 || valeur > 20) {
            throw new RuntimeException("La note doit être comprise entre 0 et 20");
        }
        
        existing.setValeur(valeur);
        existing.setCommentaire(commentaire);
        existing.setDateSaisie(LocalDateTime.now());
        // Marquer comme validée (si utilisation simplifiée)
        existing.setStatutSaisie(com.centreformation.entity.Note.StatutSaisie.VALIDEE);
        
        return noteRepository.save(existing);
    }

    @Override
    public Note saisirNote(Long etudiantId, Long coursId, Double valeur,
                          String commentaire, Long formateurId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        Formateur formateur = formateurId != null ?
                formateurRepository.findById(formateurId).orElse(null) : null;
        if (formateur == null) {
            formateur = cours.getFormateur();
        }
        
        // Vérifier si une note existe déjà
        Note existingNote = noteRepository.findByEtudiantAndCours(etudiant, cours).orElse(null);
        
        Note savedNote;
        if (existingNote != null) {
            // Mettre à jour la note existante
            existingNote.setValeur(valeur);
            existingNote.setCommentaire(commentaire);
            existingNote.setDateSaisie(LocalDateTime.now());
            existingNote.setFormateur(formateur);
            existingNote.setStatutSaisie(Note.StatutSaisie.VALIDEE);
            savedNote = noteRepository.save(existingNote);
        } else {
            // Créer une nouvelle note
            Note note = Note.builder()
                    .etudiant(etudiant)
                    .cours(cours)
                    .valeur(valeur)
                    .commentaire(commentaire)
                    .dateSaisie(LocalDateTime.now())
                    .formateur(formateur)
                    .statutSaisie(Note.StatutSaisie.VALIDEE)
                    .build();
            savedNote = noteRepository.save(note);
        }
        
        // Envoyer la notification à l'étudiant
        sendNoteNotification(etudiant, cours, valeur, commentaire);
        
        return savedNote;
    }
    
    /**
     * Envoie une notification à l'étudiant pour une nouvelle note
     */
    private void sendNoteNotification(Etudiant etudiant, Cours cours, Double valeur, String commentaire) {
        try {
            if (etudiant.getEmail() != null && !etudiant.getEmail().isEmpty()) {
                String studentName = etudiant.getPrenom() + " " + etudiant.getNom();
                emailService.notifyStudentNewNote(
                    etudiant.getEmail(),
                    studentName,
                    cours.getTitre(),
                    valeur,
                    commentaire
                );
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de note: {}", e.getMessage());
        }
    }

    @Override
    public void deleteById(Long id) {
        if (!noteRepository.existsById(id)) {
            throw new RuntimeException("Note non trouvée avec l'ID: " + id);
        }
        noteRepository.deleteById(id);
    }

    @Override
    public Double calculateAverageByEtudiant(Long etudiantId) {
        return noteRepository.findAverageByEtudiant(etudiantId);
    }

    @Override
    public Double calculateAverageByCours(Long coursId) {
        return noteRepository.findAverageByCours(coursId);
    }

    @Override
    public long count() {
        return noteRepository.count();
    }
    
    // ========== Méthodes pour les rapports PDF ==========
    
    @Override
    public List<Note> findByEtudiantId(Long etudiantId) {
        return findByEtudiant(etudiantId);
    }
    
    @Override
    public List<Note> findByCoursId(Long coursId) {
        return findByCours(coursId);
    }
    
    @Override
    public Double getMoyenneByEtudiant(Long etudiantId) {
        return calculateAverageByEtudiant(etudiantId);
    }
    
    @Override
    public Double getMoyenneByCours(Long coursId) {
        return calculateAverageByCours(coursId);
    }
    
    @Override
    public Double getTauxReussiteByCours(Long coursId) {
        List<Note> notes = findByCours(coursId);
        if (notes.isEmpty()) {
            return null;
        }
        long reussites = notes.stream()
                .filter(n -> n.getValeur() != null && n.getValeur() >= 10)
                .count();
        return (reussites * 100.0) / notes.size();
    }

    // ========== Méthodes pour la double saisie ==========

    @Override
    public Map<String, Object> effectuerDoubleSaisie(Long etudiantId, Long coursId, Double valeur,
                                                      String commentaire, Long formateurId, Integer numeroSaisie) {
        Map<String, Object> result = new HashMap<>();
        
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        
        // Validation de la note
        if (valeur < 0 || valeur > 20) {
            throw new RuntimeException("La note doit être comprise entre 0 et 20");
        }
        
        // Chercher une note existante
        Note note = noteRepository.findByEtudiantAndCours(etudiant, cours).orElse(null);
        
        if (numeroSaisie == 1) {
            // Première saisie
            if (note == null) {
                note = Note.builder()
                        .etudiant(etudiant)
                        .cours(cours)
                        .saisie1Valeur(valeur)
                        .saisie1Date(LocalDateTime.now())
                        .saisie1FormateurId(formateurId)
                        .commentaire(commentaire)
                        .statutSaisie(Note.StatutSaisie.EN_ATTENTE_SAISIE2)
                        .dateSaisie(LocalDateTime.now())
                        .build();
            } else {
                // Réinitialiser pour une nouvelle double saisie
                note.setSaisie1Valeur(valeur);
                note.setSaisie1Date(LocalDateTime.now());
                note.setSaisie1FormateurId(formateurId);
                note.setSaisie2Valeur(null);
                note.setSaisie2Date(null);
                note.setSaisie2FormateurId(null);
                note.setValeur(null);
                note.setCommentaire(commentaire);
                note.setStatutSaisie(Note.StatutSaisie.EN_ATTENTE_SAISIE2);
            }
            
            note = noteRepository.save(note);
            
            result.put("status", "SAISIE1_OK");
            result.put("message", "Première saisie enregistrée. En attente de la deuxième saisie.");
            result.put("note", note);
            
        } else if (numeroSaisie == 2) {
            // Deuxième saisie
            if (note == null || note.getStatutSaisie() != Note.StatutSaisie.EN_ATTENTE_SAISIE2) {
                throw new RuntimeException("La première saisie n'a pas été effectuée pour cet étudiant/cours");
            }
            
            note.setSaisie2Valeur(valeur);
            note.setSaisie2Date(LocalDateTime.now());
            note.setSaisie2FormateurId(formateurId);
            
            // Comparer les deux saisies
            if (note.saisiesesCorrespondent()) {
                // Les deux saisies correspondent -> valider la note
                note.setValeur(valeur);
                note.setStatutSaisie(Note.StatutSaisie.VALIDEE);
                note.setDateSaisie(LocalDateTime.now());
                
                // Envoyer la notification
                if (note.getFormateur() == null && formateurId != null) {
                    formateurRepository.findById(formateurId).ifPresent(note::setFormateur);
                }
                
                note = noteRepository.save(note);
                sendNoteNotification(etudiant, cours, valeur, commentaire);
                
                result.put("status", "VALIDEE");
                result.put("message", "Les deux saisies correspondent. Note validée: " + valeur + "/20");
                result.put("note", note);
            } else {
                // Les saisies ne correspondent pas -> conflit
                note.setStatutSaisie(Note.StatutSaisie.CONFLIT);
                note = noteRepository.save(note);
                
                result.put("status", "CONFLIT");
                result.put("message", String.format(
                    "Conflit détecté! Saisie 1: %.2f, Saisie 2: %.2f. Validation manuelle requise.",
                    note.getSaisie1Valeur(), note.getSaisie2Valeur()
                ));
                result.put("saisie1", note.getSaisie1Valeur());
                result.put("saisie2", note.getSaisie2Valeur());
                result.put("note", note);
            }
        } else {
            throw new RuntimeException("numeroSaisie doit être 1 ou 2");
        }
        
        return result;
    }

    @Override
    public Note validerConflitNote(Long noteId, Double valeurFinale) {
        Note note = findById(noteId);
        
        if (note.getStatutSaisie() != Note.StatutSaisie.CONFLIT) {
            throw new RuntimeException("Cette note n'est pas en conflit");
        }
        
        if (valeurFinale < 0 || valeurFinale > 20) {
            throw new RuntimeException("La note doit être comprise entre 0 et 20");
        }
        
        note.setValeur(valeurFinale);
        note.setStatutSaisie(Note.StatutSaisie.VALIDEE);
        note.setDateSaisie(LocalDateTime.now());
        
        Note savedNote = noteRepository.save(note);
        
        // Notifier l'étudiant
        sendNoteNotification(note.getEtudiant(), note.getCours(), valeurFinale, note.getCommentaire());
        
        return savedNote;
    }

    @Override
    public List<Note> findNotesEnConflit() {
        return noteRepository.findAll().stream()
                .filter(n -> n.getStatutSaisie() == Note.StatutSaisie.CONFLIT)
                .collect(Collectors.toList());
    }

    @Override
    public List<Note> findNotesEnAttenteSaisie2() {
        return noteRepository.findAll().stream()
                .filter(n -> n.getStatutSaisie() == Note.StatutSaisie.EN_ATTENTE_SAISIE2)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getStatutSaisieParCours(Long coursId) {
        List<Note> notes = findByCours(coursId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Note note : notes) {
            Map<String, Object> item = new HashMap<>();
            item.put("noteId", note.getId());
            item.put("etudiantId", note.getEtudiant().getId());
            item.put("etudiantNom", note.getEtudiant().getNom() + " " + note.getEtudiant().getPrenom());
            item.put("statutSaisie", note.getStatutSaisie().name());
            item.put("saisie1Valeur", note.getSaisie1Valeur());
            item.put("saisie2Valeur", note.getSaisie2Valeur());
            item.put("valeurFinale", note.getValeur());
            item.put("isValidee", note.isDoubleSaisieComplete());
            result.add(item);
        }
        
        return result;
    }
}
