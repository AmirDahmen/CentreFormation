package com.centreformation.service.impl;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Specialite;
import com.centreformation.repository.CoursRepository;
import com.centreformation.repository.FormateurRepository;
import com.centreformation.repository.SpecialiteRepository;
import com.centreformation.service.FormateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FormateurServiceImpl implements FormateurService {

    private final FormateurRepository formateurRepository;
    private final CoursRepository coursRepository;
    private final SpecialiteRepository specialiteRepository;

    @Override
    public List<Formateur> findAll() {
        return formateurRepository.findAll();
    }

    @Override
    public Page<Formateur> findAll(Pageable pageable) {
        return formateurRepository.findAll(pageable);
    }

    @Override
    public Formateur findById(Long id) {
        return formateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formateur non trouvé avec l'ID: " + id));
    }

    @Override
    public Formateur findByEmail(String email) {
        return formateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Formateur non trouvé avec l'email: " + email));
    }

    @Override
    public Page<Formateur> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageable);
        }
        return formateurRepository.searchFormateurs(keyword, pageable);
    }

    @Override
    public Page<Formateur> findBySpecialite(Long specialiteId, Pageable pageable) {
        return formateurRepository.findBySpecialiteId(specialiteId, pageable);
    }

    @Override
    public List<Cours> findCoursByFormateur(Long formateurId) {
        Formateur formateur = findById(formateurId);
        return coursRepository.findByFormateur(formateur);
    }

    @Override
    public Formateur save(Formateur formateur) {
        // Vérification unicité email
        if (formateur.getId() == null && formateurRepository.existsByEmail(formateur.getEmail())) {
            throw new RuntimeException("Un formateur avec cet email existe déjà");
        }
        
        return formateurRepository.save(formateur);
    }

    @Override
    public Formateur update(Long id, Formateur formateur) {
        Formateur existing = findById(id);
        
        // Vérification unicité email (sauf pour le formateur actuel)
        if (!existing.getEmail().equals(formateur.getEmail()) && 
            formateurRepository.existsByEmail(formateur.getEmail())) {
            throw new RuntimeException("Un formateur avec cet email existe déjà");
        }
        
        existing.setNom(formateur.getNom());
        existing.setPrenom(formateur.getPrenom());
        existing.setEmail(formateur.getEmail());
        existing.setTelephone(formateur.getTelephone());
        
        // Récupérer la spécialité si l'ID est fourni
        if (formateur.getSpecialite() != null && formateur.getSpecialite().getId() != null) {
            Specialite specialite = specialiteRepository.findById(formateur.getSpecialite().getId())
                .orElseThrow(() -> new RuntimeException("Spécialité non trouvée"));
            existing.setSpecialite(specialite);
        } else {
            existing.setSpecialite(null);
        }
        
        return formateurRepository.save(existing);
    }

    @Override
    public void deleteById(Long id) {
        if (!formateurRepository.existsById(id)) {
            throw new RuntimeException("Formateur non trouvé avec l'ID: " + id);
        }
        formateurRepository.deleteById(id);
    }

    @Override
    public long count() {
        return formateurRepository.count();
    }
}
