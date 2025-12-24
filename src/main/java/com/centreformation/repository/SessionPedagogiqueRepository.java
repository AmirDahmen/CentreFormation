package com.centreformation.repository;

import com.centreformation.entity.SessionPedagogique;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionPedagogiqueRepository extends JpaRepository<SessionPedagogique, Long> {
    
    Optional<SessionPedagogique> findByAnneeScolaireAndSemestre(String anneeScolaire, String semestre);
    
    List<SessionPedagogique> findByAnneeScolaire(String anneeScolaire);
    
    @Query("SELECT s FROM SessionPedagogique s WHERE " +
           "LOWER(s.anneeScolaire) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.semestre) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<SessionPedagogique> searchSessions(@Param("keyword") String keyword, Pageable pageable);
}
