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
    public List<Groupe> findBySessionPedagogique(Long sessionId) {
        return groupeRepository.findBySessionPedagogiqueId(sessionId, Pageable.unpaged()).getContent();
    }

    @Override
    public List<Groupe> findBySpecialite(Long specialiteId) {
        return groupeRepository.findBySpecialiteId(specialiteId, Pageable.unpaged()).getContent();
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
        
        // La relation est gérée par Etudiant (propriétaire de la relation)
        etudiant.getGroupes().add(groupe);
        etudiantRepository.save(etudiant);
    }

    @Override
    public void removeEtudiantFromGroupe(Long groupeId, Long etudiantId) {
        Groupe groupe = findById(groupeId);
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        
        // La relation est gérée par Etudiant (propriétaire de la relation)
        etudiant.getGroupes().remove(groupe);
        etudiantRepository.save(etudiant);
    }

    @Override
    public void setEtudiants(Long groupeId, Set<Long> etudiantIds) {
        Groupe groupe = findById(groupeId);
        
        // Récupérer tous les étudiants actuellement dans ce groupe
        List<Etudiant> etudiantsActuels = etudiantRepository.findAll().stream()
                .filter(e -> e.getGroupes().contains(groupe))
                .toList();
        
        // Retirer le groupe de tous les étudiants actuels
        for (Etudiant etudiant : etudiantsActuels) {
            etudiant.getGroupes().remove(groupe);
            etudiantRepository.save(etudiant);
        }
        
        // Ajouter le groupe aux nouveaux étudiants
        if (etudiantIds != null && !etudiantIds.isEmpty()) {
            List<Etudiant> nouveauxEtudiants = etudiantRepository.findAllById(etudiantIds);
            for (Etudiant etudiant : nouveauxEtudiants) {
                etudiant.getGroupes().add(groupe);
                etudiantRepository.save(etudiant);
            }
        }
    }

    @Override
    public void addCoursToGroupe(Long groupeId, Long coursId) {
        Groupe groupe = findById(groupeId);
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        
        // La relation est gérée par Cours (propriétaire de la relation)
        cours.getGroupes().add(groupe);
        coursRepository.save(cours);
    }

    @Override
    public void removeCoursFromGroupe(Long groupeId, Long coursId) {
        Groupe groupe = findById(groupeId);
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        
        // La relation est gérée par Cours (propriétaire de la relation)
        cours.getGroupes().remove(groupe);
        coursRepository.save(cours);
    }

    @Override
    public void setCours(Long groupeId, Set<Long> coursIds) {
        Groupe groupe = findById(groupeId);
        
        // Récupérer tous les cours actuellement associés à ce groupe
        List<Cours> coursActuels = coursRepository.findAll().stream()
                .filter(c -> c.getGroupes().contains(groupe))
                .toList();
        
        // Retirer le groupe de tous les cours actuels
        for (Cours cours : coursActuels) {
            cours.getGroupes().remove(groupe);
            coursRepository.save(cours);
        }
        
        // Ajouter le groupe aux nouveaux cours
        if (coursIds != null && !coursIds.isEmpty()) {
            List<Cours> nouveauxCours = coursRepository.findAllById(coursIds);
            for (Cours cours : nouveauxCours) {
                cours.getGroupes().add(groupe);
                coursRepository.save(cours);
            }
        }
    }

    @Override
    public long count() {
        return groupeRepository.count();
    }
}
