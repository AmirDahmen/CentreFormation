package com.centreformation.service;

import com.centreformation.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NoteService {

    List<Note> findAll();

    Page<Note> findAll(Pageable pageable);

    Note findById(Long id);

    List<Note> findByEtudiant(Long etudiantId);

    List<Note> findByCours(Long coursId);

    Page<Note> findByEtudiantId(Long etudiantId, Pageable pageable);

    Page<Note> findByCoursId(Long coursId, Pageable pageable);

    Note save(Note note);

    Note update(Long id, Note note);

    Note saisirNote(Long etudiantId, Long coursId, Double valeur,
                    String commentaire, Long formateurId);

    void deleteById(Long id);

    Double calculateAverageByEtudiant(Long etudiantId);

    Double calculateAverageByCours(Long coursId);

    long count();
    
    // Méthodes ajoutées pour les rapports PDF
    List<Note> findByEtudiantId(Long etudiantId);
    
    List<Note> findByCoursId(Long coursId);
    
    Double getMoyenneByEtudiant(Long etudiantId);
    
    Double getMoyenneByCours(Long coursId);
    
    Double getTauxReussiteByCours(Long coursId);
}
