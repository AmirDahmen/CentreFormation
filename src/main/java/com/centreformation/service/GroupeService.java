package com.centreformation.service;

import com.centreformation.entity.Groupe;
import com.centreformation.entity.SessionPedagogique;
import com.centreformation.entity.Specialite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface GroupeService {

    List<Groupe> findAll();

    Page<Groupe> findAll(Pageable pageable);

    Groupe findById(Long id);

    Page<Groupe> search(String keyword, Pageable pageable);

    List<Groupe> findBySessionPedagogique(SessionPedagogique session);

    List<Groupe> findBySpecialite(Specialite specialite);

    Page<Groupe> findBySessionPedagogiqueId(Long sessionId, Pageable pageable);

    Page<Groupe> findBySpecialiteId(Long specialiteId, Pageable pageable);

    Groupe save(Groupe groupe);

    Groupe update(Long id, Groupe groupe);

    void deleteById(Long id);

    void addEtudiantToGroupe(Long groupeId, Long etudiantId);

    void removeEtudiantFromGroupe(Long groupeId, Long etudiantId);

    void setEtudiants(Long groupeId, Set<Long> etudiantIds);

    void addCoursToGroupe(Long groupeId, Long coursId);

    void removeCoursFromGroupe(Long groupeId, Long coursId);

    void setCours(Long groupeId, Set<Long> coursIds);

    long count();
}
