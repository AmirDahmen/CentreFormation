package com.centreformation.service.impl;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Groupe;
import com.centreformation.entity.SessionPedagogique;
import com.centreformation.entity.Specialite;
import com.centreformation.repository.CoursRepository;
import com.centreformation.repository.EtudiantRepository;
import com.centreformation.repository.GroupeRepository;
import com.centreformation.service.GroupeService;
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
public class GroupeServiceImpl implements GroupeService {

    private final GroupeRepository groupeRepository;
    private final EtudiantRepository etudiantRepository;
    private final CoursRepository coursRepository;

    @Override
    public List<Groupe> findAll() {
        return groupeRepository.findAll();
    }

    @Override
    public Page<Groupe> findAll(Pageable pageable) {
        return groupeRepository.findAll(pageable);
    }

    @Override
    public Groupe findById(Long id) {
        return groupeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé avec l'ID: " + id));
    }

    @Override
    public Page<Groupe> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageable);
        }
        return groupeRepository.searchGroupes(keyword, pageable);
    }

    @Override
    public List<Groupe> findBySessionPedagogique(SessionPedagogique session) {
        return groupeRepository.findBySessionPedagogique(session);
    }

    @Override
    public List<Groupe> findBySpecialite(Specialite specialite) {
        return groupeRepository.findBySpecialite(specialite);
    }

    @Override
    public Page<Groupe> findBySessionPedagogiqueId(Long sessionId, Pageable pageable) {
        return groupeRepository.findBySessionPedagogiqueId(sessionId, pageable);
    }

    @Override
    public Page<Groupe> findBySpecialiteId(Long specialiteId, Pageable pageable) {
        return groupeRepository.findBySpecialiteId(specialiteId, pageable);
    }

    @Override
    public Groupe save(Groupe groupe) {
        // Vérification unicité code
        if (groupe.getCode() != null && groupe.getId() == null && 
            groupeRepository.existsByCode(groupe.getCode())) {
            throw new RuntimeException("Un groupe avec ce code existe déjà");
        }
        
        return groupeRepository.save(groupe);
    }

    @Override
    public Groupe update(Long id, Groupe groupe) {
        Groupe existing = findById(id);
        
        // Vérification unicité code (sauf pour le groupe actuel)
        if (groupe.getCode() != null && !groupe.getCode().equals(existing.getCode()) && 
            groupeRepository.existsByCode(groupe.getCode())) {
            throw new RuntimeException("Un groupe avec ce code existe déjà");
        }
        
        existing.setNom(groupe.getNom());
        existing.setCode(groupe.getCode());
        existing.setSessionPedagogique(groupe.getSessionPedagogique());
        existing.setSpecialite(groupe.getSpecialite());
        
        return groupeRepository.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        if (!groupeRepository.existsById(id)) {
            throw new RuntimeException("Groupe non trouvé avec l'ID: " + id);
        }
        groupeRepository.deleteById(id);
    }

    @Override
    public void addEtudiantToGroupe(Long groupeId, Long etudiantId) {
        Groupe groupe = findById(groupeId);
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        
        groupe.getEtudiants().add(etudiant);
        groupeRepository.save(groupe);
    }

    @Override
    public void removeEtudiantFromGroupe(Long groupeId, Long etudiantId) {
        Groupe groupe = findById(groupeId);
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        
        groupe.getEtudiants().remove(etudiant);
        groupeRepository.save(groupe);
    }

    @Override
    public void setEtudiants(Long groupeId, Set<Long> etudiantIds) {
        Groupe groupe = findById(groupeId);
        
        // Effacer les étudiants existants
        groupe.getEtudiants().clear();
        groupeRepository.flush();
        
        // Ajouter les nouveaux étudiants
        if (etudiantIds != null && !etudiantIds.isEmpty()) {
            Set<Etudiant> nouveauxEtudiants = new HashSet<>(etudiantRepository.findAllById(etudiantIds));
            groupe.getEtudiants().addAll(nouveauxEtudiants);
        }
        
        groupeRepository.save(groupe);
    }

    @Override
    public void addCoursToGroupe(Long groupeId, Long coursId) {
        Groupe groupe = findById(groupeId);
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        
        groupe.getCours().add(cours);
        groupeRepository.save(groupe);
    }

    @Override
    public void removeCoursFromGroupe(Long groupeId, Long coursId) {
        Groupe groupe = findById(groupeId);
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        
        groupe.getCours().remove(cours);
        groupeRepository.save(groupe);
    }

    @Override
    public void setCours(Long groupeId, Set<Long> coursIds) {
        Groupe groupe = findById(groupeId);
        
        // Effacer les cours existants
        groupe.getCours().clear();
        groupeRepository.flush();
        
        // Ajouter les nouveaux cours
        if (coursIds != null && !coursIds.isEmpty()) {
            Set<Cours> nouveauxCours = new HashSet<>(coursRepository.findAllById(coursIds));
            groupe.getCours().addAll(nouveauxCours);
        }
        
        groupeRepository.save(groupe);
    }

    @Override
    public long count() {
        return groupeRepository.count();
    }
}
