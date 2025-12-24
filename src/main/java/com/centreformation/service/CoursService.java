package com.centreformation.service;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Groupe;
import com.centreformation.repository.CoursRepository;
import com.centreformation.repository.FormateurRepository;
import com.centreformation.repository.GroupeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CoursService {

    private final CoursRepository coursRepository;
    private final FormateurRepository formateurRepository;
    private final GroupeRepository groupeRepository;

    public List<Cours> findAll() {
        return coursRepository.findAll();
    }

    public Page<Cours> findAll(Pageable pageable) {
        return coursRepository.findAll(pageable);
    }

    public Cours findById(Long id) {
        return coursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé avec l'ID: " + id));
    }

    public Cours findByCode(String code) {
        return coursRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé avec le code: " + code));
    }

    public Page<Cours> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageable);
        }
        return coursRepository.searchCours(keyword, pageable);
    }

    public Page<Cours> findByFormateur(Long formateurId, Pageable pageable) {
        return coursRepository.findByFormateurId(formateurId, pageable);
    }

    public List<Cours> findByGroupe(Long groupeId) {
        return coursRepository.findByGroupeId(groupeId);
    }

    public Cours save(Cours cours) {
        // Vérification unicité code
        if (cours.getId() == null && coursRepository.existsByCode(cours.getCode())) {
            throw new RuntimeException("Un cours avec ce code existe déjà");
        }
        
        return coursRepository.save(cours);
    }

    public Cours update(Long id, Cours cours) {
        Cours existing = findById(id);
        
        // Vérification unicité code (sauf pour le cours actuel)
        if (!existing.getCode().equals(cours.getCode()) && 
            coursRepository.existsByCode(cours.getCode())) {
            throw new RuntimeException("Un cours avec ce code existe déjà");
        }
        
        existing.setCode(cours.getCode());
        existing.setTitre(cours.getTitre());
        existing.setDescription(cours.getDescription());
        existing.setNombreHeures(cours.getNombreHeures());
        existing.setFormateur(cours.getFormateur());
        
        return coursRepository.save(existing);
    }

    public void deleteById(Long id) {
        if (!coursRepository.existsById(id)) {
            throw new RuntimeException("Cours non trouvé avec l'ID: " + id);
        }
        coursRepository.deleteById(id);
    }

    public void addGroupeToCours(Long coursId, Long groupeId) {
        Cours cours = findById(coursId);
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));
        
        cours.getGroupes().add(groupe);
        coursRepository.save(cours);
    }

    public void removeGroupeFromCours(Long coursId, Long groupeId) {
        Cours cours = findById(coursId);
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));
        
        cours.getGroupes().remove(groupe);
        coursRepository.save(cours);
    }

    public void setGroupes(Long coursId, Set<Long> groupeIds) {
        Cours cours = findById(coursId);
        
        // Effacer les groupes existants
        cours.getGroupes().clear();
        coursRepository.flush();
        
        // Ajouter les nouveaux groupes
        if (groupeIds != null && !groupeIds.isEmpty()) {
            Set<Groupe> nouveauxGroupes = new HashSet<>(groupeRepository.findAllById(groupeIds));
            cours.getGroupes().addAll(nouveauxGroupes);
        }
        
        coursRepository.save(cours);
    }

    public long count() {
        return coursRepository.count();
    }
}
