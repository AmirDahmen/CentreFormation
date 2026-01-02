package com.centreformation.service;

import com.centreformation.entity.Etudiant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface EtudiantService {

    List<Etudiant> findAll();

    Page<Etudiant> findAll(Pageable pageable);

    Etudiant findById(Long id);

    Etudiant findByMatricule(String matricule);

    Page<Etudiant> search(String keyword, Pageable pageable);

    Page<Etudiant> findBySpecialite(Long specialiteId, Pageable pageable);

    Etudiant save(Etudiant etudiant);

    Etudiant update(Long id, Etudiant etudiant);

    void deleteById(Long id);

    void addGroupeToEtudiant(Long etudiantId, Long groupeId);

    void removeGroupeFromEtudiant(Long etudiantId, Long groupeId);

    void setGroupes(Long etudiantId, Set<Long> groupeIds);

    long count();
}
