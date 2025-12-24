package com.centreformation.repository;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Etudiant;
import com.centreformation.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    
    List<Note> findByEtudiant(Etudiant etudiant);
    
    List<Note> findByCours(Cours cours);
    
    Optional<Note> findByEtudiantAndCours(Etudiant etudiant, Cours cours);
    
    boolean existsByEtudiantAndCours(Etudiant etudiant, Cours cours);
    
    Page<Note> findByEtudiantId(Long etudiantId, Pageable pageable);
    
    Page<Note> findByCoursId(Long coursId, Pageable pageable);
    
    @Query("SELECT AVG(n.valeur) FROM Note n WHERE n.etudiant.id = :etudiantId")
    Double findAverageByEtudiant(@Param("etudiantId") Long etudiantId);
    
    @Query("SELECT AVG(n.valeur) FROM Note n WHERE n.cours.id = :coursId")
    Double findAverageByCours(@Param("coursId") Long coursId);
}
