package com.centreformation.service;

import com.centreformation.entity.SeanceCours;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SeanceCoursService {

    List<SeanceCours> findAll();

    Page<SeanceCours> findAll(Pageable pageable);

    SeanceCours findById(Long id);

    List<SeanceCours> findByCours(Long coursId);

    List<SeanceCours> findByFormateur(Long formateurId);

    List<SeanceCours> findByGroupe(Long groupeId);

    List<SeanceCours> findByDate(LocalDate date);

    List<SeanceCours> findByDateBetween(LocalDate debut, LocalDate fin);

    List<SeanceCours> findByFormateurAndDateBetween(Long formateurId, LocalDate debut, LocalDate fin);

    List<SeanceCours> findByGroupeAndDateBetween(Long groupeId, LocalDate debut, LocalDate fin);

    /**
     * Récupère l'emploi du temps d'un étudiant pour une période donnée
     */
    List<SeanceCours> getEmploiTempsEtudiant(Long etudiantId, LocalDate debut, LocalDate fin);

    /**
     * Récupère l'emploi du temps d'un formateur pour une période donnée
     */
    List<SeanceCours> getEmploiTempsFormateur(Long formateurId, LocalDate debut, LocalDate fin);

    /**
     * Vérifie s'il y a un conflit d'horaire pour un formateur
     */
    boolean hasConflitFormateur(Long formateurId, LocalDate date, 
                                 java.time.LocalTime heureDebut, java.time.LocalTime heureFin, 
                                 Long excludeSeanceId);

    /**
     * Vérifie s'il y a un conflit d'horaire pour un groupe
     */
    boolean hasConflitGroupe(Long groupeId, LocalDate date, 
                              java.time.LocalTime heureDebut, java.time.LocalTime heureFin, 
                              Long excludeSeanceId);

    SeanceCours save(SeanceCours seance);

    SeanceCours update(Long id, SeanceCours seance);

    void deleteById(Long id);

    Page<SeanceCours> findByFormateurId(Long formateurId, Pageable pageable);

    Page<SeanceCours> findByCoursId(Long coursId, Pageable pageable);

    long count();
}
