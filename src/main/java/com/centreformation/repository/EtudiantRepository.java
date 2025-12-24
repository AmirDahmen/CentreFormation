package com.centreformation.repository;

import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Specialite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
    
    Optional<Etudiant> findByMatricule(String matricule);
    
    Optional<Etudiant> findByEmail(String email);
    
    boolean existsByMatricule(String matricule);
    
    boolean existsByEmail(String email);
    
    List<Etudiant> findBySpecialite(Specialite specialite);
    
    @Query("SELECT e FROM Etudiant e WHERE " +
           "LOWER(e.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.matricule) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Etudiant> searchEtudiants(@Param("keyword") String keyword, Pageable pageable);
    
    Page<Etudiant> findBySpecialiteId(Long specialiteId, Pageable pageable);
}
