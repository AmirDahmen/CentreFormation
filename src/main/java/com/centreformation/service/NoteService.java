package com.centreformation.service;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Note;
import com.centreformation.repository.CoursRepository;
import com.centreformation.repository.EtudiantRepository;
import com.centreformation.repository.FormateurRepository;
import com.centreformation.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;
    private final EtudiantRepository etudiantRepository;
    private final CoursRepository coursRepository;
    private final FormateurRepository formateurRepository;

    public List<Note> findAll() {
        return noteRepository.findAll();
    }

    public Page<Note> findAll(Pageable pageable) {
        return noteRepository.findAll(pageable);
    }

    public Note findById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Note non trouvée avec l'ID: " + id));
    }

    public List<Note> findByEtudiant(Long etudiantId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        return noteRepository.findByEtudiant(etudiant);
    }

    public List<Note> findByCours(Long coursId) {
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        return noteRepository.findByCours(cours);
    }

    public Page<Note> findByEtudiantId(Long etudiantId, Pageable pageable) {
        return noteRepository.findByEtudiantId(etudiantId, pageable);
    }

    public Page<Note> findByCoursId(Long coursId, Pageable pageable) {
        return noteRepository.findByCoursId(coursId, pageable);
    }

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

    public Note saisirNote(Long etudiantId, Long coursId, Double valeur, 
                          String commentaire, Long formateurId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        Formateur formateur = formateurId != null ? 
                formateurRepository.findById(formateurId).orElse(null) : null;
        
        // Vérifier si une note existe déjà
        Note existingNote = noteRepository.findByEtudiantAndCours(etudiant, cours).orElse(null);
        
        if (existingNote != null) {
            // Mettre à jour la note existante
            existingNote.setValeur(valeur);
            existingNote.setCommentaire(commentaire);
            existingNote.setDateSaisie(LocalDateTime.now());
            existingNote.setFormateur(formateur);
            return noteRepository.save(existingNote);
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
            return noteRepository.save(note);
        }
    }

    public void deleteById(Long id) {
        if (!noteRepository.existsById(id)) {
            throw new RuntimeException("Note non trouvée avec l'ID: " + id);
        }
        noteRepository.deleteById(id);
    }

    public Double calculateAverageByEtudiant(Long etudiantId) {
        return noteRepository.findAverageByEtudiant(etudiantId);
    }

    public Double calculateAverageByCours(Long coursId) {
        return noteRepository.findAverageByCours(coursId);
    }

    public long count() {
        return noteRepository.count();
    }
}
