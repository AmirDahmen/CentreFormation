package com.centreformation.service.impl;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Inscription;
import com.centreformation.repository.CoursRepository;
import com.centreformation.repository.EtudiantRepository;
import com.centreformation.repository.InscriptionRepository;
import com.centreformation.service.InscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InscriptionServiceImpl implements InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final EtudiantRepository etudiantRepository;
    private final CoursRepository coursRepository;

    @Override
    public List<Inscription> findAll() {
        return inscriptionRepository.findAll();
    }

    @Override
    public Page<Inscription> findAll(Pageable pageable) {
        return inscriptionRepository.findAll(pageable);
    }

    @Override
    public Inscription findById(Long id) {
        return inscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée avec l'ID: " + id));
    }

    @Override
    public List<Inscription> findByEtudiant(Long etudiantId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        return inscriptionRepository.findByEtudiant(etudiant);
    }

    @Override
    public List<Inscription> findByCours(Long coursId) {
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        return inscriptionRepository.findByCours(cours);
    }

    @Override
    public Page<Inscription> search(Long etudiantId, Long coursId,
                                    Inscription.StatutInscription statut, Pageable pageable) {
        return inscriptionRepository.searchInscriptions(etudiantId, coursId, statut, pageable);
    }

    @Override
    public Page<Inscription> findByStatut(Inscription.StatutInscription statut, Pageable pageable) {
        return inscriptionRepository.findByStatut(statut, pageable);
    }

    @Override
    public Inscription inscrire(Long etudiantId, Long coursId) {
        Etudiant etudiant = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> new RuntimeException("Étudiant non trouvé"));
        Cours cours = coursRepository.findById(coursId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
        
        // Vérifier si l'inscription existe déjà
        if (inscriptionRepository.existsByEtudiantAndCours(etudiant, cours)) {
            throw new RuntimeException("L'étudiant est déjà inscrit à ce cours");
        }
        
        Inscription inscription = Inscription.builder()
                .etudiant(etudiant)
                .cours(cours)
                .dateInscription(LocalDateTime.now())
                .statut(Inscription.StatutInscription.ACTIVE)
                .build();
        
        return inscriptionRepository.save(inscription);
    }

    @Override
    public Inscription save(Inscription inscription) {
        // Vérifier si l'inscription existe déjà
        if (inscription.getId() == null && 
            inscriptionRepository.existsByEtudiantAndCours(inscription.getEtudiant(), inscription.getCours())) {
            throw new RuntimeException("L'étudiant est déjà inscrit à ce cours");
        }
        
        return inscriptionRepository.save(inscription);
    }

    @Override
    public Inscription update(Long id, Inscription inscription) {
        Inscription existing = findById(id);
        existing.setStatut(inscription.getStatut());
        existing.setDateInscription(inscription.getDateInscription());
        return inscriptionRepository.save(existing);
    }

    @Override
    public void annuler(Long inscriptionId) {
        Inscription inscription = findById(inscriptionId);
        inscription.setStatut(Inscription.StatutInscription.ANNULEE);
        inscriptionRepository.save(inscription);
    }

    @Override
    public void deleteById(Long id) {
        if (!inscriptionRepository.existsById(id)) {
            throw new RuntimeException("Inscription non trouvée avec l'ID: " + id);
        }
        inscriptionRepository.deleteById(id);
    }

    @Override
    public long count() {
        return inscriptionRepository.count();
    }

    @Override
    public long countByStatut(Inscription.StatutInscription statut) {
        return inscriptionRepository.findByStatut(statut).size();
    }
}
