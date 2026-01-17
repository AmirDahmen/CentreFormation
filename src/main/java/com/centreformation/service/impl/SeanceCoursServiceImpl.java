package com.centreformation.service.impl;

import com.centreformation.entity.*;
import com.centreformation.repository.*;
import com.centreformation.service.SeanceCoursService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SeanceCoursServiceImpl implements SeanceCoursService {

    private final SeanceCoursRepository seanceCoursRepository;
    private final CoursRepository coursRepository;
    private final FormateurRepository formateurRepository;
    private final GroupeRepository groupeRepository;
    private final EtudiantRepository etudiantRepository;

    @Override
    public List<SeanceCours> findAll() {
        return seanceCoursRepository.findAll();
    }

    @Override
    public Page<SeanceCours> findAll(Pageable pageable) {
        return seanceCoursRepository.findAll(pageable);
    }

    @Override
    public SeanceCours findById(Long id) {
        return seanceCoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Séance non trouvée avec l'ID: " + id));
    }

    @Override
    public List<SeanceCours> findByCours(Long coursId) {
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        return seanceCoursRepository.findByCours(cours);
    }

    @Override
    public List<SeanceCours> findByFormateur(Long formateurId) {
        Formateur formateur = formateurRepository.findById(formateurId)
                .orElseThrow(() -> new RuntimeException("Formateur non trouvé"));
        return seanceCoursRepository.findByFormateur(formateur);
    }

    @Override
    public List<SeanceCours> findByGroupe(Long groupeId) {
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));
        return seanceCoursRepository.findByGroupe(groupe);
    }

    @Override
    public List<SeanceCours> findByDate(LocalDate date) {
        return seanceCoursRepository.findByDate(date);
    }

    @Override
    public List<SeanceCours> findByDateBetween(LocalDate debut, LocalDate fin) {
        return seanceCoursRepository.findByDateBetween(debut, fin);
    }

    @Override
    public List<SeanceCours> findByFormateurAndDateBetween(Long formateurId, LocalDate debut, LocalDate fin) {
        return seanceCoursRepository.findByFormateurAndDateBetween(formateurId, debut, fin);
    }

    @Override
    public List<SeanceCours> getEmploiTempsEtudiant(Long etudiantId, LocalDate debut, LocalDate fin) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        
        // Récupérer tous les groupes de l'étudiant
        Set<Groupe> groupes = etudiant.getGroupes();
        
        // Récupérer toutes les séances pour ces groupes dans la période
        Set<SeanceCours> seances = new HashSet<>();
        for (Groupe groupe : groupes) {
            List<SeanceCours> seancesGroupe = seanceCoursRepository.findByGroupe(groupe);
            seances.addAll(seancesGroupe.stream()
                    .filter(s -> !s.getDate().isBefore(debut) && !s.getDate().isAfter(fin))
                    .collect(Collectors.toList()));
        }
        
        return new ArrayList<>(seances).stream()
                .sorted((s1, s2) -> {
                    int dateCompare = s1.getDate().compareTo(s2.getDate());
                    if (dateCompare != 0) return dateCompare;
                    return s1.getHeureDebut().compareTo(s2.getHeureDebut());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SeanceCours> getEmploiTempsFormateur(Long formateurId, LocalDate debut, LocalDate fin) {
        return seanceCoursRepository.findByFormateurAndDateBetween(formateurId, debut, fin)
                .stream()
                .sorted((s1, s2) -> {
                    int dateCompare = s1.getDate().compareTo(s2.getDate());
                    if (dateCompare != 0) return dateCompare;
                    return s1.getHeureDebut().compareTo(s2.getHeureDebut());
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasConflitFormateur(Long formateurId, LocalDate date, 
                                        LocalTime heureDebut, LocalTime heureFin, 
                                        Long excludeSeanceId) {
        List<SeanceCours> seances = seanceCoursRepository.findByFormateurAndDateBetween(formateurId, date, date);
        
        for (SeanceCours seance : seances) {
            // Exclure la séance actuelle lors d'une mise à jour
            if (excludeSeanceId != null && seance.getId().equals(excludeSeanceId)) {
                continue;
            }
            
            // Vérifier le chevauchement
            if (isOverlapping(seance.getHeureDebut(), seance.getHeureFin(), heureDebut, heureFin)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasConflitGroupe(Long groupeId, LocalDate date, 
                                     LocalTime heureDebut, LocalTime heureFin, 
                                     Long excludeSeanceId) {
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));
        
        List<SeanceCours> seances = seanceCoursRepository.findByGroupe(groupe);
        
        for (SeanceCours seance : seances) {
            // Même date?
            if (!seance.getDate().equals(date)) {
                continue;
            }
            
            // Exclure la séance actuelle lors d'une mise à jour
            if (excludeSeanceId != null && seance.getId().equals(excludeSeanceId)) {
                continue;
            }
            
            // Vérifier le chevauchement
            if (isOverlapping(seance.getHeureDebut(), seance.getHeureFin(), heureDebut, heureFin)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si deux créneaux horaires se chevauchent
     */
    private boolean isOverlapping(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    @Override
    public SeanceCours save(SeanceCours seance) {
        // Vérifier les conflits
        validateNoConflicts(seance, null);
        return seanceCoursRepository.save(seance);
    }

    @Override
    public SeanceCours update(Long id, SeanceCours seance) {
        SeanceCours existing = findById(id);
        
        // Vérifier les conflits
        validateNoConflicts(seance, id);
        
        existing.setDate(seance.getDate());
        existing.setHeureDebut(seance.getHeureDebut());
        existing.setHeureFin(seance.getHeureFin());
        existing.setSalle(seance.getSalle());
        existing.setContenu(seance.getContenu());
        existing.setCours(seance.getCours());
        existing.setGroupe(seance.getGroupe());
        existing.setFormateur(seance.getFormateur());
        
        return seanceCoursRepository.save(existing);
    }

    private void validateNoConflicts(SeanceCours seance, Long excludeId) {
        // Vérifier conflit formateur
        if (seance.getFormateur() != null) {
            if (hasConflitFormateur(seance.getFormateur().getId(), seance.getDate(), 
                    seance.getHeureDebut(), seance.getHeureFin(), excludeId)) {
                throw new RuntimeException("Conflit d'horaire : Le formateur a déjà une séance à ce créneau");
            }
        }
        
        // Vérifier conflit groupe
        if (seance.getGroupe() != null) {
            if (hasConflitGroupe(seance.getGroupe().getId(), seance.getDate(), 
                    seance.getHeureDebut(), seance.getHeureFin(), excludeId)) {
                throw new RuntimeException("Conflit d'horaire : Le groupe a déjà une séance à ce créneau");
            }
        }
    }

    @Override
    public void deleteById(Long id) {
        if (!seanceCoursRepository.existsById(id)) {
            throw new RuntimeException("Séance non trouvée avec l'ID: " + id);
        }
        seanceCoursRepository.deleteById(id);
    }

    @Override
    public Page<SeanceCours> findByFormateurId(Long formateurId, Pageable pageable) {
        return seanceCoursRepository.findByFormateurId(formateurId, pageable);
    }

    @Override
    public Page<SeanceCours> findByCoursId(Long coursId, Pageable pageable) {
        return seanceCoursRepository.findByCoursId(coursId, pageable);
    }

    @Override
    public long count() {
        return seanceCoursRepository.count();
    }
}
