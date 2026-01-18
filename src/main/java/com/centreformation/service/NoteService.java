package com.centreformation.service;

import com.centreformation.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

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

    /**
     * Met à jour simplement la valeur et le commentaire d'une note
     */
    Note updateSimple(Long id, Double valeur, String commentaire);

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

    // === Méthodes pour la double saisie ===
    
    /**
     * Effectue une saisie (première ou deuxième) dans le processus de double saisie
     * @param etudiantId ID de l'étudiant
     * @param coursId ID du cours
     * @param valeur Valeur de la note
     * @param commentaire Commentaire optionnel
     * @param formateurId ID du formateur qui saisit
     * @param numeroSaisie 1 pour première saisie, 2 pour deuxième
     * @return Map avec le résultat (statut, message, note)
     */
    Map<String, Object> effectuerDoubleSaisie(Long etudiantId, Long coursId, Double valeur,
                                               String commentaire, Long formateurId, Integer numeroSaisie);

    /**
     * Valide manuellement une note en conflit (résolution par admin)
     */
    Note validerConflitNote(Long noteId, Double valeurFinale);

    /**
     * Récupère les notes en conflit
     */
    List<Note> findNotesEnConflit();

    /**
     * Récupère les notes en attente de deuxième saisie
     */
    List<Note> findNotesEnAttenteSaisie2();

    /**
     * Récupère le statut de saisie pour toutes les notes d'un cours
     */
    List<Map<String, Object>> getStatutSaisieParCours(Long coursId);
}
