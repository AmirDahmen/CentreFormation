package com.centreformation.repository;

import com.centreformation.entity.Cours;
import com.centreformation.entity.Formateur;
import com.centreformation.entity.Groupe;
import com.centreformation.entity.SeanceCours;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SeanceCoursRepository extends JpaRepository<SeanceCours, Long> {
    
    List<SeanceCours> findByCours(Cours cours);
    
    List<SeanceCours> findByFormateur(Formateur formateur);
    
    List<SeanceCours> findByGroupe(Groupe groupe);
    
    List<SeanceCours> findByDate(LocalDate date);
    
    @Query("SELECT s FROM SeanceCours s WHERE s.date BETWEEN :debut AND :fin")
    List<SeanceCours> findByDateBetween(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
    
    @Query("SELECT s FROM SeanceCours s WHERE s.formateur.id = :formateurId AND s.date BETWEEN :debut AND :fin")
    List<SeanceCours> findByFormateurAndDateBetween(
        @Param("formateurId") Long formateurId,
        @Param("debut") LocalDate debut,
        @Param("fin") LocalDate fin
    );
    
    Page<SeanceCours> findByFormateurId(Long formateurId, Pageable pageable);
    
    Page<SeanceCours> findByCoursId(Long coursId, Pageable pageable);
}
