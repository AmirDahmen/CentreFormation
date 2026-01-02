package com.centreformation.service;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Formateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FormateurService {

    List<Formateur> findAll();

    Page<Formateur> findAll(Pageable pageable);

    Formateur findById(Long id);

    Formateur findByEmail(String email);

    Page<Formateur> search(String keyword, Pageable pageable);

    Page<Formateur> findBySpecialite(Long specialiteId, Pageable pageable);

    List<Cours> findCoursByFormateur(Long formateurId);

    Formateur save(Formateur formateur);

    Formateur update(Long id, Formateur formateur);

    void deleteById(Long id);

    long count();
}
