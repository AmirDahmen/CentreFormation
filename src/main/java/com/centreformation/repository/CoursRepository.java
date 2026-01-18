package com.centreformation.repository;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Formateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoursRepository extends JpaRepository<Cours, Long> {
    
    Optional<Cours> findByCode(String code);
    
    boolean existsByCode(String code);
    
    List<Cours> findByFormateur(Formateur formateur);
    
    @Query("SELECT c FROM Cours c WHERE " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.titre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Cours> searchCours(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT c FROM Cours c JOIN c.groupes g WHERE g.id = :groupeId")
    List<Cours> findByGroupeId(@Param("groupeId") Long groupeId);
    
    @Query("SELECT c FROM Cours c WHERE c.formateur.id = :formateurId")
    Page<Cours> findByFormateurId(@Param("formateurId") Long formateurId, Pageable pageable);
    
    @Query("SELECT c FROM Cours c LEFT JOIN FETCH c.formateur WHERE c.formateur.id = :formateurId")
    List<Cours> findAllByFormateurId(@Param("formateurId") Long formateurId);
}
