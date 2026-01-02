package com.centreformation.service.impl;

import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Groupe;
import com.centreformation.entity.Specialite;
import com.centreformation.repository.EtudiantRepository;
import com.centreformation.repository.GroupeRepository;
import com.centreformation.repository.SpecialiteRepository;
import com.centreformation.service.EtudiantService;
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
public class EtudiantServiceImpl implements EtudiantService {

    private final EtudiantRepository etudiantRepository;
    private final GroupeRepository groupeRepository;
    private final SpecialiteRepository specialiteRepository;

    @Override
    public List<Etudiant> findAll() {
        return etudiantRepository.findAll();
    }

    @Override
    public Page<Etudiant> findAll(Pageable pageable) {
        return etudiantRepository.findAll(pageable);
    }

    @Override
    public Etudiant findById(Long id) {
        return etudiantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé avec l'ID: " + id));
    }

    @Override
    public Etudiant findByMatricule(String matricule) {
        return etudiantRepository.findByMatricule(matricule)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé avec le matricule: " + matricule));
    }

    @Override
    public Page<Etudiant> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageable);
        }
        return etudiantRepository.searchEtudiants(keyword, pageable);
    }

    @Override
    public Page<Etudiant> findBySpecialite(Long specialiteId, Pageable pageable) {
        return etudiantRepository.findBySpecialiteId(specialiteId, pageable);
    }

    @Override
    public Etudiant save(Etudiant etudiant) {
        // Vérification unicité matricule
        if (etudiant.getId() == null && etudiantRepository.existsByMatricule(etudiant.getMatricule())) {
            throw new RuntimeException("Un étudiant avec ce matricule existe déjà");
        }
        
        // Vérification unicité email
        if (etudiant.getId() == null && etudiantRepository.existsByEmail(etudiant.getEmail())) {
            throw new RuntimeException("Un étudiant avec cet email existe déjà");
        }
        
        return etudiantRepository.save(etudiant);
    }

    @Override
    public Etudiant update(Long id, Etudiant etudiant) {
        Etudiant existing = findById(id);
        
        // Vérification unicité matricule (sauf pour l'étudiant actuel)
        if (!existing.getMatricule().equals(etudiant.getMatricule()) && 
            etudiantRepository.existsByMatricule(etudiant.getMatricule())) {
            throw new RuntimeException("Un étudiant avec ce matricule existe déjà");
        }
        
        // Vérification unicité email (sauf pour l'étudiant actuel)
        if (!existing.getEmail().equals(etudiant.getEmail()) && 
            etudiantRepository.existsByEmail(etudiant.getEmail())) {
            throw new RuntimeException("Un étudiant avec cet email existe déjà");
        }
        
        existing.setMatricule(etudiant.getMatricule());
        existing.setNom(etudiant.getNom());
        existing.setPrenom(etudiant.getPrenom());
        existing.setEmail(etudiant.getEmail());
        existing.setDateInscription(etudiant.getDateInscription());
        
        // Récupérer la spécialité si l'ID est fourni
        if (etudiant.getSpecialite() != null && etudiant.getSpecialite().getId() != null) {
            Specialite specialite = specialiteRepository.findById(etudiant.getSpecialite().getId())
                .orElseThrow(() -> new RuntimeException("Spécialité non trouvée"));
            existing.setSpecialite(specialite);
        } else {
            existing.setSpecialite(null);
        }
        
        return etudiantRepository.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        if (!etudiantRepository.existsById(id)) {
            throw new RuntimeException("Étudiant non trouvé avec l'ID: " + id);
        }
        etudiantRepository.deleteById(id);
    }

    @Override
    public void addGroupeToEtudiant(Long etudiantId, Long groupeId) {
        Etudiant etudiant = findById(etudiantId);
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));
        
        etudiant.getGroupes().add(groupe);
        etudiantRepository.save(etudiant);
    }

    @Override
    public void removeGroupeFromEtudiant(Long etudiantId, Long groupeId) {
        Etudiant etudiant = findById(etudiantId);
        Groupe groupe = groupeRepository.findById(groupeId)
                .orElseThrow(() -> new RuntimeException("Groupe non trouvé"));
        
        etudiant.getGroupes().remove(groupe);
        etudiantRepository.save(etudiant);
    }

    @Override
    public void setGroupes(Long etudiantId, Set<Long> groupeIds) {
        Etudiant etudiant = findById(etudiantId);
        
        // Effacer les groupes existants
        etudiant.getGroupes().clear();
        etudiantRepository.flush();
        
        // Ajouter les nouveaux groupes
        if (groupeIds != null && !groupeIds.isEmpty()) {
            Set<Groupe> nouveauxGroupes = new HashSet<>(groupeRepository.findAllById(groupeIds));
            etudiant.getGroupes().addAll(nouveauxGroupes);
        }
        
        etudiantRepository.save(etudiant);
    }

    @Override
    public long count() {
        return etudiantRepository.count();
    }
}
