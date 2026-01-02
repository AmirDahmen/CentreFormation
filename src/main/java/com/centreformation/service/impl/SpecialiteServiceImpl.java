package com.centreformation.service.impl;

import com.centreformation.entity.Specialite;
import com.centreformation.repository.SpecialiteRepository;
import com.centreformation.service.SpecialiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SpecialiteServiceImpl implements SpecialiteService {

    private final SpecialiteRepository specialiteRepository;

    @Override
    public List<Specialite> findAll() {
        return specialiteRepository.findAll();
    }

    @Override
    public Page<Specialite> findAll(Pageable pageable) {
        return specialiteRepository.findAll(pageable);
    }

    @Override
    public Specialite findById(Long id) {
        return specialiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Spécialité non trouvée avec l'ID: " + id));
    }

    @Override
    public Page<Specialite> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageable);
        }
        return specialiteRepository.searchSpecialites(keyword, pageable);
    }

    @Override
    public Specialite save(Specialite specialite) {
        // Vérification unicité nom
        if (specialite.getId() == null && specialiteRepository.existsByNom(specialite.getNom())) {
            throw new RuntimeException("Une spécialité avec ce nom existe déjà");
        }
        
        return specialiteRepository.save(specialite);
    }

    @Override
    public Specialite update(Long id, Specialite specialite) {
        Specialite existing = findById(id);
        
        // Vérification unicité nom (sauf pour la spécialité actuelle)
        if (!existing.getNom().equals(specialite.getNom()) && 
            specialiteRepository.existsByNom(specialite.getNom())) {
            throw new RuntimeException("Une spécialité avec ce nom existe déjà");
        }
        
        existing.setNom(specialite.getNom());
        existing.setDescription(specialite.getDescription());
        
        return specialiteRepository.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        if (!specialiteRepository.existsById(id)) {
            throw new RuntimeException("Spécialité non trouvée avec l'ID: " + id);
        }
        specialiteRepository.deleteById(id);
    }

    @Override
    public long count() {
        return specialiteRepository.count();
    }
}
