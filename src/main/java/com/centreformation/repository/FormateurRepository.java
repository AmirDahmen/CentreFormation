package com.centreformation.repository;

import com.centreformation.entity.Formateur;
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
public interface FormateurRepository extends JpaRepository<Formateur, Long> {
    
    Optional<Formateur> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<Formateur> findBySpecialite(Specialite specialite);
    
    @Query("SELECT f FROM Formateur f WHERE " +
           "LOWER(f.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.prenom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Formateur> searchFormateurs(@Param("keyword") String keyword, Pageable pageable);
    
    Page<Formateur> findBySpecialiteId(Long specialiteId, Pageable pageable);
}
