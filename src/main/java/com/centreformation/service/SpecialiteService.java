package com.centreformation.service;

import com.centreformation.entity.Specialite;
import com.centreformation.repository.SpecialiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SpecialiteService {

    private final SpecialiteRepository specialiteRepository;

    public List<Specialite> findAll() {
        return specialiteRepository.findAll();
    }

    public Page<Specialite> findAll(Pageable pageable) {
        return specialiteRepository.findAll(pageable);
    }

    public Specialite findById(Long id) {
        return specialiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Spécialité non trouvée avec l'ID: " + id));
    }

    public Page<Specialite> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageable);
        }
        return specialiteRepository.searchSpecialites(keyword, pageable);
    }

    public Specialite save(Specialite specialite) {
        // Vérification unicité nom
        if (specialite.getId() == null && specialiteRepository.existsByNom(specialite.getNom())) {
            throw new RuntimeException("Une spécialité avec ce nom existe déjà");
        }
        
        return specialiteRepository.save(specialite);
    }

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

    public void deleteById(Long id) {
        if (!specialiteRepository.existsById(id)) {
            throw new RuntimeException("Spécialité non trouvée avec l'ID: " + id);
        }
        specialiteRepository.deleteById(id);
    }

    public long count() {
        return specialiteRepository.count();
    }
}
