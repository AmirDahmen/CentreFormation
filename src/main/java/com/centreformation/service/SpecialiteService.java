package com.centreformation.service;

import com.centreformation.entity.Specialite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SpecialiteService {

    List<Specialite> findAll();

    Page<Specialite> findAll(Pageable pageable);

    Specialite findById(Long id);

    Page<Specialite> search(String keyword, Pageable pageable);

    Specialite save(Specialite specialite);

    Specialite update(Long id, Specialite specialite);

    void deleteById(Long id);

    long count();
}
