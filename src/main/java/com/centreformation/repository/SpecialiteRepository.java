package com.centreformation.repository;

import com.centreformation.entity.Specialite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialiteRepository extends JpaRepository<Specialite, Long> {
    
    Optional<Specialite> findByNom(String nom);
    
    boolean existsByNom(String nom);
    
    @Query("SELECT s FROM Specialite s WHERE " +
           "LOWER(s.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Specialite> searchSpecialites(@Param("keyword") String keyword, Pageable pageable);
}
