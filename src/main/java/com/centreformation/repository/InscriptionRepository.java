package com.centreformation.repository;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Inscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscriptionRepository extends JpaRepository<Inscription, Long> {
    
    List<Inscription> findByEtudiant(Etudiant etudiant);
    
    List<Inscription> findByCours(Cours cours);
    
    Optional<Inscription> findByEtudiantAndCours(Etudiant etudiant, Cours cours);
    
    boolean existsByEtudiantAndCours(Etudiant etudiant, Cours cours);
    
    @Query("SELECT i FROM Inscription i WHERE i.statut = :statut")
    List<Inscription> findByStatut(@Param("statut") Inscription.StatutInscription statut);
    
    Page<Inscription> findByEtudiantId(Long etudiantId, Pageable pageable);
    
    Page<Inscription> findByCoursId(Long coursId, Pageable pageable);
    
    Page<Inscription> findByStatut(Inscription.StatutInscription statut, Pageable pageable);
    
    @Query("SELECT i FROM Inscription i WHERE " +
           "(:etudiantId IS NULL OR i.etudiant.id = :etudiantId) AND " +
           "(:coursId IS NULL OR i.cours.id = :coursId) AND " +
           "(:statut IS NULL OR i.statut = :statut)")
    Page<Inscription> searchInscriptions(
        @Param("etudiantId") Long etudiantId,
        @Param("coursId") Long coursId,
        @Param("statut") Inscription.StatutInscription statut,
        Pageable pageable
    );
}
