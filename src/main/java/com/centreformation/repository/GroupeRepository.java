package com.centreformation.repository;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Groupe;
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
public interface GroupeRepository extends JpaRepository<Groupe, Long> {
    
    Optional<Groupe> findByCode(String code);
    
    boolean existsByCode(String code);
    
    // Trouver les groupes d'un cours
    List<Groupe> findByCours(Cours cours);
    
    List<Groupe> findByCoursId(Long coursId);
    
    List<Groupe> findBySpecialite(Specialite specialite);
    
    // Trouver les groupes par session pédagogique (via cours)
    @Query("SELECT g FROM Groupe g WHERE g.cours.sessionPedagogique.id = :sessionId")
    Page<Groupe> findBySessionPedagogiqueId(@Param("sessionId") Long sessionId, Pageable pageable);
    
    Page<Groupe> findBySpecialiteId(Long specialiteId, Pageable pageable);
    
    Page<Groupe> findByCoursId(Long coursId, Pageable pageable);
    
    @Query("SELECT g FROM Groupe g WHERE " +
           "LOWER(g.nom) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(g.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Groupe> searchGroupes(@Param("keyword") String keyword, Pageable pageable);
}
