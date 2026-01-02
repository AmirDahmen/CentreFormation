package com.centreformation.service;

import com.centreformation.entity.Inscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InscriptionService {

    List<Inscription> findAll();

    Page<Inscription> findAll(Pageable pageable);

    Inscription findById(Long id);

    List<Inscription> findByEtudiant(Long etudiantId);

    List<Inscription> findByCours(Long coursId);

    Page<Inscription> search(Long etudiantId, Long coursId,
                             Inscription.StatutInscription statut, Pageable pageable);

    Page<Inscription> findByStatut(Inscription.StatutInscription statut, Pageable pageable);

    Inscription inscrire(Long etudiantId, Long coursId);

    Inscription save(Inscription inscription);

    Inscription update(Long id, Inscription inscription);

    void annuler(Long inscriptionId);

    void deleteById(Long id);

    long count();

    long countByStatut(Inscription.StatutInscription statut);
}
