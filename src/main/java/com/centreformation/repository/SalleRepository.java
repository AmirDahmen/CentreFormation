package com.centreformation.repository;

import com.centreformation.entity.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {
    
    Optional<Salle> findByCode(String code);
    
    List<Salle> findByActifTrueOrderByCodeAsc();
    
    List<Salle> findByTypeAndActifTrueOrderByCodeAsc(String type);
    
    // Trouver les salles disponibles pour un créneau donné
    @Query("""
        SELECT s FROM Salle s 
        WHERE s.actif = true 
        AND s.id NOT IN (
            SELECT sc.salle.id FROM SeanceCours sc 
            WHERE sc.salle IS NOT NULL
            AND sc.date = :date 
            AND (
                (sc.heureDebut < :heureFin AND sc.heureFin > :heureDebut)
            )
        )
        ORDER BY s.code
    """)
    List<Salle> findSallesDisponibles(
        @Param("date") LocalDate date,
        @Param("heureDebut") LocalTime heureDebut,
        @Param("heureFin") LocalTime heureFin
    );
    
    // Trouver les salles disponibles (exclure une séance spécifique pour modification)
    @Query("""
        SELECT s FROM Salle s 
        WHERE s.actif = true 
        AND s.id NOT IN (
            SELECT sc.salle.id FROM SeanceCours sc 
            WHERE sc.salle IS NOT NULL
            AND sc.id != :excludeSeanceId
            AND sc.date = :date 
            AND (
                (sc.heureDebut < :heureFin AND sc.heureFin > :heureDebut)
            )
        )
        ORDER BY s.code
    """)
    List<Salle> findSallesDisponiblesExcluding(
        @Param("date") LocalDate date,
        @Param("heureDebut") LocalTime heureDebut,
        @Param("heureFin") LocalTime heureFin,
        @Param("excludeSeanceId") Long excludeSeanceId
    );
    
    // Vérifier si une salle est disponible pour un créneau
    @Query("""
        SELECT COUNT(sc) = 0 FROM SeanceCours sc 
        WHERE sc.salle.id = :salleId 
        AND sc.date = :date 
        AND (sc.heureDebut < :heureFin AND sc.heureFin > :heureDebut)
    """)
    boolean isSalleDisponible(
        @Param("salleId") Long salleId,
        @Param("date") LocalDate date,
        @Param("heureDebut") LocalTime heureDebut,
        @Param("heureFin") LocalTime heureFin
    );
}
