package com.centreformation.service;

import com.centreformation.entity.Cours;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface CoursService {

    List<Cours> findAll();

    Page<Cours> findAll(Pageable pageable);

    Cours findById(Long id);

    Cours findByCode(String code);

    Page<Cours> search(String keyword, Pageable pageable);

    Page<Cours> findByFormateur(Long formateurId, Pageable pageable);

    List<Cours> findByGroupe(Long groupeId);

    Cours save(Cours cours);

    Cours update(Long id, Cours cours);

    void deleteById(Long id);

    void addGroupeToCours(Long coursId, Long groupeId);

    void removeGroupeFromCours(Long coursId, Long groupeId);

    void setGroupes(Long coursId, Set<Long> groupeIds);

    long count();
}
