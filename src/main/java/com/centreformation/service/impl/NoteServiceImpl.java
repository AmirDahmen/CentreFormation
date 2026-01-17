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
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        return noteRepository.findByCours(cours);
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
                .filter(n -> n.getValeur() >= 10)
                .count();
        return (reussites * 100.0) / notes.size();
    }
}
